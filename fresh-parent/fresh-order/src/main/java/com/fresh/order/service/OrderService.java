package com.fresh.order.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fresh.common.base.Result;
import com.fresh.common.constant.OrderConstant;
import com.fresh.common.dto.mq.CommentAddMqDTO;
import com.fresh.common.dto.mq.OrderEventMqDTO;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.common.util.ContextUtil;
import com.fresh.common.util.IdUtils;
import com.fresh.common.util.JsonUtils;
import com.fresh.order.dto.*;
import com.fresh.order.entity.AfterSale;
import com.fresh.order.entity.OrderItem;
import com.fresh.order.entity.OrderMain;
import com.fresh.order.entity.PayLog;
import com.fresh.order.feign.GoodsFeignClient;
import com.fresh.order.feign.UserFeignClient;
import com.fresh.order.mapper.AfterSaleMapper;
import com.fresh.order.mapper.OrderItemMapper;
import com.fresh.order.mapper.OrderMainMapper;
import com.fresh.order.mapper.PayLogMapper;
import com.fresh.order.vo.AfterSaleAdminVO;
import com.fresh.order.vo.OrderDetailVO;
import com.fresh.order.vo.UserAddressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final AfterSaleMapper afterSaleMapper;
    private final PayLogMapper payLogMapper;
    private final UserFeignClient userFeignClient;
    private final GoodsFeignClient goodsFeignClient;
    private final OrderMqProducer orderMqProducer;

    @SentinelResource("createOrder")
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(OrderCreateDTO dto) {
        Long userId = requireUserId();
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "结算商品为空");
        }
        Result<UserAddressVO> addressResult = userFeignClient.getAddressById(dto.getAddressId());
        if (addressResult.getData() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "收货地址不存在");
        }
        UserAddressVO address = addressResult.getData();

        // 服务端重算价格，不信任前端传入的 price
        OrderSettleVO settle = settleItems(dto.getItems(), dto.getUserCouponId());
        BigDecimal goodsTotal = settle.getTotalAmount();
        BigDecimal payAmount = settle.getPayAmount();

        if (dto.getIntegralUsed() != null && dto.getIntegralUsed() > 0) {
            IntegralDTO integralDTO = new IntegralDTO();
            integralDTO.setUserId(userId);
            integralDTO.setIntegral(dto.getIntegralUsed());
            userFeignClient.freezeIntegral(integralDTO);
        }

        OrderMain order = new OrderMain();
        order.setOrderNo(IdUtils.nextIdStr());
        order.setUserId(userId);
        order.setTotalAmount(goodsTotal);
        order.setPayAmount(payAmount);
        order.setStatus(OrderConstant.STATUS_UNPAID);
        order.setAddressId(dto.getAddressId());
        order.setReceiverName(address.getName());
        order.setReceiverPhone(address.getPhone());
        order.setCommunity(address.getCommunity());
        order.setDetailAddress(address.getDetailAddr());
        order.setIntegralUsedCount(dto.getIntegralUsed() == null ? 0 : dto.getIntegralUsed());
        order.setCouponId(settle.getSelectedUserCouponId());
        order.setCouponDeduct(settle.getCouponDeduct() == null ? BigDecimal.ZERO : settle.getCouponDeduct());
        order.setFullreduceDeduct(settle.getFullreduceDeduct() == null ? BigDecimal.ZERO : settle.getFullreduceDeduct());
        order.setDelFlag(0);

        // 单活动订单回填主表活动 ID
        for (OrderSettleVO.Item it : settle.getItems()) {
            if (it.getActivityType() != null && it.getActivityType() == OrderConstant.ACTIVITY_GROUP && it.getActivityId() != null) {
                order.setGroupActivityId(it.getActivityId());
            }
            if (it.getActivityType() != null && it.getActivityType() == OrderConstant.ACTIVITY_SECKILL && it.getActivityId() != null) {
                order.setSeckillActivityId(it.getActivityId());
            }
        }
        orderMainMapper.insert(order);

        if (settle.getSelectedUserCouponId() != null
                && settle.getCouponDeduct() != null
                && settle.getCouponDeduct().compareTo(BigDecimal.ZERO) > 0) {
            CouponUseDTO useDTO = new CouponUseDTO();
            useDTO.setUserId(userId);
            useDTO.setUserCouponId(settle.getSelectedUserCouponId());
            useDTO.setOrderNo(order.getOrderNo());
            useDTO.setOrderAmount(goodsTotal);
            Result<BigDecimal> useResult = goodsFeignClient.useCoupon(useDTO);
            if (useResult == null || useResult.getCode() == null || useResult.getCode() != 200) {
                String msg = useResult != null && useResult.getMsg() != null ? useResult.getMsg() : "优惠券核销失败";
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), msg);
            }
        }

        for (OrderSettleVO.Item priced : settle.getItems()) {
            StockChangeDTO stock = new StockChangeDTO();
            stock.setGoodsId(priced.getGoodsId());
            stock.setSpecId(priced.getSpecId());
            stock.setNum(priced.getNum());
            stock.setActivityType(priced.getActivityType());
            stock.setActivityId(priced.getActivityId());
            goodsFeignClient.deductStock(stock);

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setGoodsId(priced.getGoodsId());
            item.setSpecId(priced.getSpecId());
            item.setActivityType(priced.getActivityType() == null ? 1 : priced.getActivityType());
            item.setActivityId(priced.getActivityId());
            item.setGoodsName(priced.getGoodsName());
            item.setGoodsImg(priced.getGoodsImg());
            item.setPrice(priced.getPrice());
            item.setNum(priced.getNum());
            item.setSubTotal(priced.getSubTotal());
            item.setIsCommented(0);
            orderItemMapper.insert(item);
        }

        OrderEventMqDTO event = buildOrderEvent(order);
        orderMqProducer.sendOrderCreate(event);
        orderMqProducer.sendOrderUnpaidDelay(event);
        return order.getOrderNo();
    }

    /** 结算预览：服务端计价（含优惠券） */
    public OrderSettleVO settlePreview(OrderCreateDTO dto) {
        requireUserId();
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "结算商品为空");
        }
        return settleItems(dto.getItems(), dto.getUserCouponId());
    }

    private OrderSettleVO settleItems(List<OrderItemDTO> rawItems, Long userCouponId) {
        OrderSettleVO settle = new OrderSettleVO();
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (OrderItemDTO itemDto : rawItems) {
            if (itemDto.getGoodsId() == null || itemDto.getSpecId() == null) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品或规格不能为空");
            }
            int num = itemDto.getNum() == null || itemDto.getNum() <= 0 ? 1 : itemDto.getNum();
            GoodsPriceQueryDTO query = new GoodsPriceQueryDTO();
            query.setGoodsId(itemDto.getGoodsId());
            query.setSpecId(itemDto.getSpecId());
            query.setActivityType(itemDto.getActivityType());
            query.setActivityId(itemDto.getActivityId());
            Result<GoodsPriceVO> priceResult = goodsFeignClient.resolvePrice(query);
            if (priceResult == null || priceResult.getCode() == null || priceResult.getCode() != 200
                    || priceResult.getData() == null || priceResult.getData().getPrice() == null) {
                String msg = priceResult != null && priceResult.getMsg() != null ? priceResult.getMsg() : "商品计价失败";
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), msg);
            }
            GoodsPriceVO priced = priceResult.getData();
            BigDecimal sub = priced.getPrice().multiply(BigDecimal.valueOf(num));
            total = total.add(sub);
            count += num;

            OrderSettleVO.Item line = new OrderSettleVO.Item();
            line.setGoodsId(priced.getGoodsId());
            line.setSpecId(priced.getSpecId());
            line.setGoodsName(priced.getGoodsName());
            line.setGoodsImg(priced.getGoodsImg());
            line.setPrice(priced.getPrice());
            line.setNum(num);
            line.setSubTotal(sub);
            line.setActivityType(priced.getActivityType());
            line.setActivityId(priced.getActivityId());
            settle.getItems().add(line);
        }

        BigDecimal couponDeduct = BigDecimal.ZERO;
        BigDecimal fullreduceDeduct = BigDecimal.ZERO;
        Long selectedCouponId = null;
        List<UserCouponVO> available = List.of();

        try {
            PromotionQueryDTO promoQuery = new PromotionQueryDTO();
            promoQuery.setUserId(requireUserId());
            promoQuery.setOrderAmount(total);
            Result<PromotionCalcVO> promoResult = goodsFeignClient.calcPromotion(promoQuery);
            if (promoResult != null && promoResult.getCode() != null && promoResult.getCode() == 200
                    && promoResult.getData() != null) {
                PromotionCalcVO promo = promoResult.getData();
                available = promo.getAvailableCoupons() == null ? List.of() : promo.getAvailableCoupons();
                if (userCouponId != null) {
                    UserCouponVO matched = available.stream()
                            .filter(c -> userCouponId.equals(c.getId()))
                            .findFirst()
                            .orElse(null);
                    if (matched == null) {
                        throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "所选优惠券不可用");
                    }
                    couponDeduct = matched.getReduceAmount() == null ? BigDecimal.ZERO : matched.getReduceAmount();
                    selectedCouponId = matched.getId();
                }
            } else if (userCouponId != null) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "优惠券查询失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("优惠券计价失败，按无券结算: {}", e.getMessage());
            if (userCouponId != null) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "优惠券查询失败");
            }
            available = List.of();
        }

        if (couponDeduct.compareTo(total) > 0) {
            couponDeduct = total;
        }
        BigDecimal pay = total.subtract(couponDeduct);
        if (pay.compareTo(BigDecimal.ZERO) < 0) {
            pay = BigDecimal.ZERO;
        }

        settle.setTotalAmount(total);
        settle.setPayAmount(pay);
        settle.setTotalGoodsCount(count);
        settle.setCouponDeduct(couponDeduct);
        settle.setFullreduceDeduct(fullreduceDeduct);
        settle.setSelectedUserCouponId(selectedCouponId);
        settle.setAvailableCoupons(available);
        return settle;
    }

    /** 管理端：订单列表 */
    public List<OrderMain> listAllForAdmin(Integer status) {
        requireAdminId();
        LambdaQueryWrapper<OrderMain> qw = new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getDelFlag, 0)
                .orderByDesc(OrderMain::getCreateTime);
        if (status != null) {
            qw.eq(OrderMain::getStatus, status);
        }
        return orderMainMapper.selectList(qw);
    }

    public OrderDetailVO adminDetail(String orderNo) {
        requireAdminId();
        OrderMain order = getByOrderNo(orderNo);
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(order);
        vo.setItems(items);
        return vo;
    }

    /** 管理端核销：待自提 → 已完成，并发放完成积分 */
    @Transactional(rollbackFor = Exception.class)
    public void completePickup(String orderNo) {
        requireAdminId();
        OrderMain order = getByOrderNo(orderNo);
        if (order.getStatus() != OrderConstant.STATUS_WAIT_PICKUP) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "仅待自提订单可核销");
        }
        order.setStatus(OrderConstant.STATUS_COMPLETED);
        orderMainMapper.updateById(order);
        grantOrderCompleteIntegral(order);
        log.info("订单核销完成 orderNo={}", orderNo);
    }

    /** 管理端：配送到站，待配送 → 待自提 */
    @Transactional(rollbackFor = Exception.class)
    public void markArrived(String orderNo) {
        requireAdminId();
        OrderMain order = getByOrderNo(orderNo);
        if (order.getStatus() != OrderConstant.STATUS_WAIT_DELIVERY) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "仅待配送订单可确认到站");
        }
        order.setStatus(OrderConstant.STATUS_WAIT_PICKUP);
        orderMainMapper.updateById(order);
        log.info("订单配送到站 orderNo={}", orderNo);
    }

    /** 管理端：售后工单列表 */
    public List<AfterSaleAdminVO> listAfterSaleForAdmin(Integer auditStatus) {
        requireAdminId();
        LambdaQueryWrapper<AfterSale> qw = new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getDelFlag, 0)
                .orderByAsc(AfterSale::getAuditStatus)
                .orderByDesc(AfterSale::getCreateTime);
        if (auditStatus != null) {
            qw.eq(AfterSale::getAuditStatus, auditStatus);
        }
        return enrichAfterSaleList(afterSaleMapper.selectList(qw));
    }

    /** 用户端：我的售后工单 */
    public List<AfterSaleAdminVO> listAfterSaleForUser() {
        Long userId = requireUserId();
        List<AfterSale> list = afterSaleMapper.selectList(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getUserId, userId)
                .eq(AfterSale::getDelFlag, 0)
                .orderByDesc(AfterSale::getCreateTime));
        return enrichAfterSaleList(list);
    }

    private List<AfterSaleAdminVO> enrichAfterSaleList(List<AfterSale> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = list.stream().map(AfterSale::getOrderItemId).filter(Objects::nonNull).distinct().toList();
        Map<Long, OrderItem> itemMap = itemIds.isEmpty() ? Map.of()
                : orderItemMapper.selectBatchIds(itemIds).stream().collect(Collectors.toMap(OrderItem::getId, i -> i, (a, b) -> a));
        List<Long> orderIds = itemMap.values().stream().map(OrderItem::getOrderId).filter(Objects::nonNull).distinct().toList();
        Map<Long, OrderMain> orderMap = orderIds.isEmpty() ? Map.of()
                : orderMainMapper.selectBatchIds(orderIds).stream().collect(Collectors.toMap(OrderMain::getId, o -> o, (a, b) -> a));

        List<AfterSaleAdminVO> result = new ArrayList<>(list.size());
        for (AfterSale as : list) {
            AfterSaleAdminVO vo = toAfterSaleAdminVO(as);
            OrderItem item = itemMap.get(as.getOrderItemId());
            if (item != null) {
                vo.setOrderId(item.getOrderId());
                vo.setGoodsName(item.getGoodsName());
                vo.setGoodsImg(item.getGoodsImg());
                vo.setItemPrice(item.getPrice());
                vo.setItemNum(item.getNum());
                OrderMain order = orderMap.get(item.getOrderId());
                if (order != null) {
                    vo.setOrderNo(order.getOrderNo());
                    vo.setOrderStatus(order.getStatus());
                }
            }
            result.add(vo);
        }
        return result;
    }

    /** 待审核售后数量（管理端红点/提醒） */
    public long countPendingAfterSale() {
        requireAdminId();
        return afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getDelFlag, 0)
                .eq(AfterSale::getAuditStatus, 0));
    }

    /** 管理端审核售后：通过/驳回 */
    @Transactional(rollbackFor = Exception.class)
    public void auditAfterSale(AfterSaleAuditDTO dto) {
        Long adminId = requireAdminId();
        if (dto == null || dto.getId() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "售后工单ID不能为空");
        }
        Integer status = dto.getAuditStatus();
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "审核状态仅支持 1通过 / 2驳回");
        }
        AfterSale as = afterSaleMapper.selectById(dto.getId());
        if (as == null || (as.getDelFlag() != null && as.getDelFlag() == 1)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "售后工单不存在");
        }
        if (as.getAuditStatus() != null && as.getAuditStatus() != 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "该工单已审核，请勿重复操作");
        }

        as.setAuditStatus(status);
        as.setAuditAdminId(adminId);
        // 保留用户申请备注，审核意见追加其后
        if (dto.getRemark() != null && !dto.getRemark().isBlank()) {
            String old = as.getRemark();
            as.setRemark(old == null || old.isBlank()
                    ? dto.getRemark()
                    : old + "；审核：" + dto.getRemark());
        }
        as.setUpdateTime(LocalDateTime.now());
        if (status == 1) {
            BigDecimal refund = dto.getActualRefundMoney();
            if (refund == null) {
                refund = as.getAiRefundMoney();
            }
            if (refund == null) {
                refund = BigDecimal.ZERO;
            }
            if (refund.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "退款金额不能为负");
            }
            as.setActualRefundMoney(refund);
            as.setRefundTime(LocalDateTime.now());
        }
        afterSaleMapper.updateById(as);
        // 审核通过：保持「售后中」，不再回到已完成（避免出现在用户「已完成」列表）
        // 审核驳回：若无其它待审售后，恢复为已完成
        if (status == 2) {
            restoreOrderAfterReject(as.getOrderItemId());
        }
        log.info("售后审核完成 id={} status={} adminId={}", as.getId(), status, adminId);
    }

    private void restoreOrderAfterReject(Long orderItemId) {
        if (orderItemId == null) {
            return;
        }
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            return;
        }
        OrderMain order = orderMainMapper.selectById(item.getOrderId());
        if (order == null || order.getStatus() != OrderConstant.STATUS_AFTER_SALE) {
            return;
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        List<Long> itemIds = items.stream().map(OrderItem::getId).toList();
        if (itemIds.isEmpty()) {
            return;
        }
        Long pending = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getDelFlag, 0)
                .eq(AfterSale::getAuditStatus, 0)
                .in(AfterSale::getOrderItemId, itemIds));
        if (pending != null && pending > 0) {
            return;
        }
        Long approved = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getDelFlag, 0)
                .eq(AfterSale::getAuditStatus, 1)
                .in(AfterSale::getOrderItemId, itemIds));
        if (approved != null && approved > 0) {
            // 同订单仍有通过的售后，保持售后中
            return;
        }
        order.setStatus(OrderConstant.STATUS_COMPLETED);
        orderMainMapper.updateById(order);
        log.info("售后驳回且无通过记录，订单恢复已完成 orderNo={}", order.getOrderNo());
    }

    private AfterSaleAdminVO toAfterSaleAdminVO(AfterSale as) {
        AfterSaleAdminVO vo = new AfterSaleAdminVO();
        vo.setId(as.getId());
        vo.setOrderItemId(as.getOrderItemId());
        vo.setUserId(as.getUserId());
        vo.setGoodsId(as.getGoodsId());
        vo.setDamageImg(as.getDamageImg());
        vo.setAiDamageLevel(as.getAiDamageLevel());
        vo.setAiRate(as.getAiRate());
        vo.setAiRefundMoney(as.getAiRefundMoney());
        vo.setActualRefundMoney(as.getActualRefundMoney());
        vo.setAuditStatus(as.getAuditStatus());
        vo.setAuditAdminId(as.getAuditAdminId());
        vo.setRefundTime(as.getRefundTime());
        vo.setRemark(as.getRemark());
        vo.setCreateTime(as.getCreateTime());
        return vo;
    }

    @SentinelResource("payCallback")
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo) {
        paySuccess(orderNo, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo, String outTradeNo, Map<String, String> callbackData) {
        OrderMain order = getByOrderNo(orderNo);
        // 幂等：已支付及之后状态重复回调直接忽略
        if (order.getStatus() == OrderConstant.STATUS_WAIT_DELIVERY
                || order.getStatus() == OrderConstant.STATUS_WAIT_PICKUP
                || order.getStatus() == OrderConstant.STATUS_COMPLETED) {
            log.info("订单已支付，忽略重复回调 orderNo={}", orderNo);
            return;
        }
        if (order.getStatus() != OrderConstant.STATUS_UNPAID) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "订单状态异常");
        }
        Long ctxUserId = ContextUtil.getUserId();
        if (ctxUserId != null && !ctxUserId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN);
        }
        order.setStatus(OrderConstant.STATUS_WAIT_DELIVERY);
        order.setPayTime(LocalDateTime.now());
        order.setPayType(1);
        orderMainMapper.updateById(order);

        PayLog payLog = new PayLog();
        payLog.setOrderId(order.getId());
        payLog.setOrderNo(orderNo);
        payLog.setOutTradeNo(outTradeNo != null ? outTradeNo : "mock_" + IdUtils.nextIdStr());
        payLog.setPayAmount(order.getPayAmount());
        payLog.setPayStatus(1);
        payLog.setCallbackContent(callbackData != null ? JsonUtils.toJson(callbackData) : null);
        payLogMapper.insert(payLog);

        if (order.getIntegralUsedCount() != null && order.getIntegralUsedCount() > 0) {
            IntegralDTO dto = new IntegralDTO();
            dto.setUserId(order.getUserId());
            dto.setIntegral(-order.getIntegralUsedCount());
            dto.setOrderId(order.getId());
            dto.setRemark("订单支付扣减积分");
            userFeignClient.changeIntegral(dto);
        }

        log.info("支付成功 orderNo={}, outTradeNo={}, amount={}",
                orderNo, payLog.getOutTradeNo(), order.getPayAmount());
        orderMqProducer.sendOrderSuccess(buildOrderEvent(order));
    }

    public void handleUnpaidTimeout(String payload) {
        OrderEventMqDTO dto = JSON.parseObject(payload, OrderEventMqDTO.class);
        if (dto == null || dto.getOrderNo() == null) {
            return;
        }
        log.info("消费 ORDER_UNPAID_TOPIC, orderNo={}", dto.getOrderNo());
        cancelUnpaidOrder(dto.getOrderNo());
    }

    public void handleCommentAdd(String payload) {
        CommentAddMqDTO dto = JSON.parseObject(payload, CommentAddMqDTO.class);
        if (dto == null || dto.getOrderItemId() == null) {
            return;
        }
        log.info("消费 COMMENT_ADD_TOPIC, orderItemId={}", dto.getOrderItemId());
        markOrderItemCommented(dto.getOrderItemId());
    }

    public void markOrderItemCommented(Long orderItemId) {
        if (orderItemId == null) {
            return;
        }
        orderItemMapper.update(null, new LambdaUpdateWrapper<OrderItem>()
                .eq(OrderItem::getId, orderItemId)
                .set(OrderItem::getIsCommented, 1));
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelUnpaidOrder(String orderNo) {
        OrderMain order = getByOrderNo(orderNo);
        if (order.getStatus() != OrderConstant.STATUS_UNPAID) {
            return;
        }
        order.setStatus(OrderConstant.STATUS_CANCELLED);
        order.setTimeoutCancel(1);
        orderMainMapper.updateById(order);

        if (order.getIntegralUsedCount() != null && order.getIntegralUsedCount() > 0) {
            IntegralDTO dto = new IntegralDTO();
            dto.setUserId(order.getUserId());
            dto.setIntegral(order.getIntegralUsedCount());
            dto.setOrderId(order.getId());
            userFeignClient.unfreezeIntegral(dto);
        }

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            StockChangeDTO stock = new StockChangeDTO();
            stock.setGoodsId(item.getGoodsId());
            stock.setSpecId(item.getSpecId());
            stock.setNum(item.getNum());
            stock.setActivityType(item.getActivityType());
            stock.setActivityId(item.getActivityId());
            goodsFeignClient.restoreStock(stock);
        }
    }

    public List<OrderMain> listByUser() {
        return orderMainMapper.selectList(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getUserId, requireUserId())
                .eq(OrderMain::getDelFlag, 0)
                .orderByDesc(OrderMain::getCreateTime));
    }

    /** 用户订单列表（含商品行，供列表缩略图） */
    public List<OrderDetailVO> listByUserWithItems() {
        List<OrderMain> orders = listByUser();
        List<OrderDetailVO> result = new java.util.ArrayList<>();
        for (OrderMain order : orders) {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId()));
            OrderDetailVO vo = new OrderDetailVO();
            vo.setOrder(order);
            vo.setItems(items);
            result.add(vo);
        }
        return result;
    }

    public OrderMain detail(String orderNo) {
        OrderMain order = getByOrderNo(orderNo);
        if (!order.getUserId().equals(requireUserId())) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN);
        }
        return order;
    }

    public OrderDetailVO detailWithItems(String orderNo) {
        OrderMain order = detail(orderNo);
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(order);
        vo.setItems(items);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void applyAfterSale(AfterSale afterSale) {
        Long userId = requireUserId();
        afterSale.setUserId(userId);
        afterSale.setAuditStatus(0);
        afterSale.setDelFlag(0);
        afterSaleMapper.insert(afterSale);

        // 通过订单项关联订单，标记为售后中
        if (afterSale.getOrderItemId() != null) {
            OrderItem item = orderItemMapper.selectById(afterSale.getOrderItemId());
            if (item != null) {
                OrderMain order = orderMainMapper.selectById(item.getOrderId());
                if (order != null && order.getUserId().equals(userId)
                        && (order.getStatus() == OrderConstant.STATUS_WAIT_DELIVERY
                        || order.getStatus() == OrderConstant.STATUS_WAIT_PICKUP
                        || order.getStatus() == OrderConstant.STATUS_COMPLETED)) {
                    order.setStatus(OrderConstant.STATUS_AFTER_SALE);
                    orderMainMapper.updateById(order);
                }
            }
        }
        if (afterSale.getDamageImg() != null && !afterSale.getDamageImg().isBlank()) {
            orderMqProducer.sendAfterSaleImage(afterSale.getId(), afterSale.getUserId(), afterSale.getDamageImg());
        }
    }

    public void updateAfterSaleAiResult(AfterSaleAiResultDTO dto) {
        AfterSale afterSale = afterSaleMapper.selectById(dto.getAfterSaleId());
        if (afterSale == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "售后工单不存在");
        }
        afterSale.setAiDamageLevel(dto.getAiDamageLevel());
        afterSale.setAiRate(dto.getAiRate());
        afterSale.setAiRefundMoney(dto.getAiRefundMoney());
        afterSale.setUpdateTime(LocalDateTime.now());
        afterSaleMapper.updateById(afterSale);
    }

    public OrderMain getByOrderNo(String orderNo) {
        OrderMain order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "订单不存在");
        }
        return order;
    }

    public OrderItemCheckVO checkCanComment(Long orderItemId) {
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "订单项不存在");
        }
        OrderMain order = orderMainMapper.selectById(item.getOrderId());
        OrderItemCheckVO vo = new OrderItemCheckVO();
        vo.setOrderItemId(orderItemId);
        vo.setGoodsId(item.getGoodsId());
        vo.setSpecId(item.getSpecId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setOrderStatus(order.getStatus());
        vo.setIsCommented(item.getIsCommented());
        vo.setCanComment(order.getStatus() == OrderConstant.STATUS_COMPLETED
                && (item.getIsCommented() == null || item.getIsCommented() == 0));
        return vo;
    }

    /** 测试/后台：将订单标记为已完成以便评价 */
    @Transactional(rollbackFor = Exception.class)
    public void completeOrderForTest(String orderNo) {
        OrderMain order = getByOrderNo(orderNo);
        order.setStatus(OrderConstant.STATUS_COMPLETED);
        orderMainMapper.updateById(order);
        grantOrderCompleteIntegral(order);
    }

    /** 订单完成后发放积分：按实付金额 1 元≈1 分，最少 1，最多 500；用户侧按 orderId 幂等 */
    private void grantOrderCompleteIntegral(OrderMain order) {
        if (order == null || order.getUserId() == null) {
            return;
        }
        int reward = calcOrderRewardIntegral(order.getPayAmount());
        try {
            IntegralDTO dto = new IntegralDTO();
            dto.setUserId(order.getUserId());
            dto.setIntegral(reward);
            dto.setOrderId(order.getId());
            dto.setRemark("订单完成奖励积分");
            userFeignClient.changeIntegral(dto);
            log.info("订单完成发放积分 orderNo={} reward={}", order.getOrderNo(), reward);
        } catch (Exception e) {
            log.warn("订单完成发放积分失败 orderNo={} reward={}", order.getOrderNo(), reward, e);
        }
    }

    private int calcOrderRewardIntegral(BigDecimal payAmount) {
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 1;
        }
        int points = payAmount.setScale(0, java.math.RoundingMode.DOWN).intValue();
        if (points < 1) {
            return 1;
        }
        return Math.min(points, 500);
    }

    private OrderEventMqDTO buildOrderEvent(OrderMain order) {
        OrderEventMqDTO event = new OrderEventMqDTO();
        event.setOrderNo(order.getOrderNo());
        event.setOrderId(order.getId());
        event.setUserId(order.getUserId());
        event.setPayAmount(order.getPayAmount());
        event.setOperateUserId(order.getUserId());
        return event;
    }

    private Long requireUserId() {
        Long userId = ContextUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }

    private Long requireAdminId() {
        Long adminId = ContextUtil.getAdminId();
        if (adminId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED.getCode(), "需要管理员登录");
        }
        return adminId;
    }
}
