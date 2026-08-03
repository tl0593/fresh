package com.fresh.goods.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fresh.common.base.PageVO;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.common.util.ContextUtil;
import com.fresh.common.util.JsonUtils;
import com.fresh.common.util.RedisUtils;
import com.fresh.goods.config.GoodsProperties;
import com.fresh.goods.constant.GoodsRedisKeyConstant;
import com.fresh.goods.dto.CouponReceiveDTO;
import com.fresh.goods.dto.CouponUseDTO;
import com.fresh.goods.dto.IntegralExchangeDTO;
import com.fresh.goods.dto.PromotionQueryDTO;
import com.fresh.goods.dto.SeckillCouponDTO;
import com.fresh.common.base.Result;
import com.fresh.goods.dto.UserIntegralDTO;
import com.fresh.goods.entity.promotion.*;
import com.fresh.goods.feign.UserFeignClient;
import com.fresh.goods.mapper.promotion.*;
import com.fresh.goods.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final CouponTemplateMapper templateMapper;
    private final UserCouponMapper userCouponMapper;
    private final FullReduceActivityMapper fullReduceMapper;
    private final CouponUseLogMapper couponUseLogMapper;
    private final IntegralCouponMapper integralCouponMapper;
    private final SeckillCouponMapper seckillCouponMapper;
    private final IntegralLotteryPrizeMapper lotteryPrizeMapper;
    private final GoodsRedisService goodsRedisService;
    private final RedisUtils redisUtils;
    private final GoodsProperties properties;
    private final GoodsMqProducer mqProducer;
    private final UserFeignClient userFeignClient;

    public List<CouponTemplateVO> templateList() {
        LocalDateTime now = LocalDateTime.now();
        List<CouponTemplate> list = templateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStatus, 1)
                .eq(CouponTemplate::getDelFlag, 0)
                .le(CouponTemplate::getStartTime, now)
                .ge(CouponTemplate::getEndTime, now));
        return list.stream().map(this::toTemplateVO).collect(Collectors.toList());
    }

    @SentinelResource("receiveCoupon")
    @Transactional(transactionManager = "promotionTransactionManager", rollbackFor = Exception.class)
    public void receive(CouponReceiveDTO dto) {
        Long userId = dto.getUserId() != null ? dto.getUserId() : requireUserId();
        Long templateId = dto.getTemplateId();
        String lockKey = GoodsRedisKeyConstant.LOCK_COUPON + templateId + ":" + userId;
        if (!goodsRedisService.tryLock(lockKey, properties.getLockWaitTime(), properties.getLockHoldTime())) {
            throw new BusinessException(ErrorCodeEnum.TOO_MANY_REQUESTS);
        }
        try {
            CouponTemplate template = templateMapper.selectById(templateId);
            validateTemplate(template);
            int received = countUserReceived(userId, templateId);
            if (template.getLimitType() == 1 && received >= template.getLimitNum()) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "已达到领取上限");
            }
            int remain = template.getTotalCount() - countReceived(templateId);
            if (remain <= 0) {
                throw new BusinessException(ErrorCodeEnum.COUPON_EMPTY);
            }
            UserCoupon userCoupon = createUserCoupon(userId, template);
            userCouponMapper.insert(userCoupon);
            goodsRedisService.incrementUserLimit(GoodsRedisKeyConstant.COUPON_USER_LIMIT + userId + ":" + templateId);
            redisUtils.delete(GoodsRedisKeyConstant.COUPON_USER_VALID + userId);
            mqProducer.sendCouponReceive(mqProducer.buildCouponPayload(userId, templateId, userCoupon.getId()));
        } finally {
            goodsRedisService.unlock(lockKey);
        }
    }

    public List<SeckillCoupon> seckillCouponList() {
        LocalDateTime now = LocalDateTime.now();
        return seckillCouponMapper.selectList(new LambdaQueryWrapper<SeckillCoupon>()
                .eq(SeckillCoupon::getStatus, 1)
                .eq(SeckillCoupon::getDelFlag, 0)
                .le(SeckillCoupon::getActivityStart, now)
                .ge(SeckillCoupon::getActivityEnd, now));
    }

    /** 用户端：整点抢券列表（含券信息与开抢状态） */
    public List<SeckillCouponVO> seckillCouponVoList() {
        return seckillCouponList().stream().map(this::toSeckillVO).collect(Collectors.toList());
    }

    /** 管理端：全部抢券配置 */
    public List<SeckillCoupon> adminSeckillCouponList() {
        return seckillCouponMapper.selectList(new LambdaQueryWrapper<SeckillCoupon>()
                .eq(SeckillCoupon::getDelFlag, 0)
                .orderByDesc(SeckillCoupon::getId));
    }

    public void seckillReceive(SeckillCouponDTO dto) {
        Long userId = dto.getUserId() != null ? dto.getUserId() : requireUserId();
        Long actId = dto.getActId();
        SeckillCoupon act = seckillCouponMapper.selectById(actId);
        if (act == null || act.getStatus() != 1) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "抢券活动未开始或已结束");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(act.getActivityStart()) || now.isAfter(act.getActivityEnd())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "不在活动周期内");
        }
        if (now.getHour() != act.getStartHour()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "未到开抢时间");
        }

        String stockKey = GoodsRedisKeyConstant.SECKILL_COUPON_STOCK + actId;
        String userKey = GoodsRedisKeyConstant.SECKILL_COUPON_USER + actId + ":" + userId;
        // 库存未预热时按剩余库存兜底写入
        if (goodsRedisService.getStock(stockKey) == null) {
            int remain = Math.max(0, act.getTotalStock() - (act.getUsedNum() == null ? 0 : act.getUsedNum()));
            if (remain > 0) {
                goodsRedisService.setStock(stockKey, remain);
            }
        }
        long ttl = ChronoUnit.SECONDS.between(now, LocalDateTime.of(LocalDate.now(), LocalTime.MAX));
        Long result = goodsRedisService.seckillCouponGrab(stockKey, userKey, ttl);
        if (result == null || result == -1) {
            throw new BusinessException(ErrorCodeEnum.COUPON_EMPTY);
        }
        if (result == -2) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "今日已抢过");
        }

        CouponTemplate template = templateMapper.selectById(act.getTemplateId());
        if (template == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "优惠券不存在");
        }
        UserCoupon userCoupon = createUserCoupon(userId, template);
        userCouponMapper.insert(userCoupon);
        act.setUsedNum((act.getUsedNum() == null ? 0 : act.getUsedNum()) + 1);
        seckillCouponMapper.updateById(act);
        mqProducer.sendCouponReceive(mqProducer.buildCouponPayload(userId, act.getTemplateId(), userCoupon.getId()));
    }

    public List<IntegralCoupon> integralCouponList() {
        return integralCouponMapper.selectList(new LambdaQueryWrapper<IntegralCoupon>()
                .eq(IntegralCoupon::getStatus, 1)
                .eq(IntegralCoupon::getDelFlag, 0));
    }

    /** 用户端：积分兑券列表（含券信息） */
    public List<IntegralCouponVO> integralCouponVoList() {
        return integralCouponList().stream().map(this::toIntegralVO).collect(Collectors.toList());
    }

    /** 管理端：全部兑券配置 */
    public List<IntegralCoupon> adminIntegralCouponList() {
        return integralCouponMapper.selectList(new LambdaQueryWrapper<IntegralCoupon>()
                .eq(IntegralCoupon::getDelFlag, 0)
                .orderByDesc(IntegralCoupon::getId));
    }

    @Transactional(transactionManager = "promotionTransactionManager", rollbackFor = Exception.class)
    public void exchangeCoupon(IntegralExchangeDTO dto) {
        Long userId = dto.getUserId() != null ? dto.getUserId() : requireUserId();
        IntegralCoupon config = integralCouponMapper.selectById(dto.getIntegralCouponId());
        if (config == null || config.getStatus() != 1) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "积分兑换券已下架");
        }
        if (config.getUsedNum() >= config.getTotalStock()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "积分兑换券已售罄");
        }
        long todayCount = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getTemplateId, config.getTemplateId())
                .ge(UserCoupon::getReceiveTime, LocalDate.now().atStartOfDay()));
        if (todayCount >= config.getDailyLimit()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "已达今日兑换上限");
        }
        CouponTemplate template = templateMapper.selectById(config.getTemplateId());
        if (template == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "优惠券不存在");
        }

        deductUserIntegral(userId, config.getCostIntegral(), "积分兑换优惠券:" + template.getCouponName());
        try {
            UserCoupon userCoupon = createUserCoupon(userId, template);
            userCouponMapper.insert(userCoupon);
            config.setUsedNum((config.getUsedNum() == null ? 0 : config.getUsedNum()) + 1);
            integralCouponMapper.updateById(config);
            redisUtils.delete(GoodsRedisKeyConstant.INTEGRAL_COUPON_STOCK + config.getId());
            mqProducer.sendCouponReceive(mqProducer.buildCouponPayload(userId, config.getTemplateId(), userCoupon.getId()));
        } catch (RuntimeException e) {
            refundUserIntegral(userId, config.getCostIntegral(), "兑券失败退回积分");
            throw e;
        }
    }

    public List<LotteryPrizeVO> lotteryPrizeList() {
        List<IntegralLotteryPrize> prizes = lotteryPrizeMapper.selectList(new LambdaQueryWrapper<IntegralLotteryPrize>()
                .eq(IntegralLotteryPrize::getStatus, 1)
                .eq(IntegralLotteryPrize::getDelFlag, 0));
        return prizes.stream().map(this::toLotteryPrizeVO).collect(Collectors.toList());
    }

    /** 积分抽奖：先扣积分，再按权重开奖并发奖 */
    public LotteryDrawResultVO lotteryDraw() {
        Long userId = requireUserId();
        List<IntegralLotteryPrize> prizes = lotteryPrizeMapper.selectList(new LambdaQueryWrapper<IntegralLotteryPrize>()
                .eq(IntegralLotteryPrize::getStatus, 1)
                .eq(IntegralLotteryPrize::getDelFlag, 0));
        if (prizes.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "暂无抽奖活动");
        }
        int cost = prizes.stream()
                .map(IntegralLotteryPrize::getCostIntegral)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .min()
                .orElse(0);
        if (cost <= 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "抽奖消耗未配置");
        }
        deductUserIntegral(userId, cost, "积分抽奖消耗");

        IntegralLotteryPrize hit;
        try {
            hit = weightedPick(prizes);
            if (hit == null) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "抽奖失败");
            }
            LotteryDrawResultVO vo = new LotteryDrawResultVO();
            vo.setPrizeId(hit.getId());
            vo.setRewardType(hit.getRewardType());
            vo.setRewardIntegral(hit.getRewardIntegral());
            vo.setRewardCouponId(hit.getRewardCouponId());
            vo.setCostIntegral(cost);

            if (hit.getRewardType() != null && hit.getRewardType() == 1) {
                int reward = hit.getRewardIntegral() == null ? 0 : hit.getRewardIntegral();
                if (reward > 0) {
                    changeUserIntegral(userId, reward, "积分抽奖奖励");
                }
                vo.setRewardName(reward + "积分");
                vo.setMessage("恭喜获得 " + reward + " 积分");
            } else if (hit.getRewardType() != null && hit.getRewardType() == 2) {
                CouponTemplate template = templateMapper.selectById(hit.getRewardCouponId());
                if (template == null) {
                    throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "奖品券不存在");
                }
                UserCoupon userCoupon = createUserCoupon(userId, template);
                userCouponMapper.insert(userCoupon);
                mqProducer.sendCouponReceive(mqProducer.buildCouponPayload(userId, template.getId(), userCoupon.getId()));
                vo.setRewardName(template.getCouponName());
                vo.setMessage("恭喜获得优惠券：" + template.getCouponName());
            } else {
                vo.setRewardName("谢谢参与");
                vo.setMessage("很遗憾，未中奖");
            }
            return vo;
        } catch (RuntimeException e) {
            refundUserIntegral(userId, cost, "抽奖失败退回积分");
            throw e;
        }
    }

    /** 一键领取当前可领的全部模板券 */
    public BatchReceiveResultVO receiveBatch() {
        Long userId = requireUserId();
        List<CouponTemplateVO> templates = templateList();
        int ok = 0;
        int fail = 0;
        for (CouponTemplateVO t : templates) {
            if (t.getRemainCount() != null && t.getRemainCount() <= 0) {
                fail++;
                continue;
            }
            try {
                CouponReceiveDTO dto = new CouponReceiveDTO();
                dto.setUserId(userId);
                dto.setTemplateId(t.getId());
                receive(dto);
                ok++;
            } catch (Exception e) {
                fail++;
                log.debug("一键领券跳过 templateId={} msg={}", t.getId(), e.getMessage());
            }
        }
        BatchReceiveResultVO vo = new BatchReceiveResultVO();
        vo.setSuccessCount(ok);
        vo.setFailCount(fail);
        if (ok == 0) {
            vo.setMessage(fail == 0 ? "暂无可领优惠券" : "没有新的可领优惠券");
        } else {
            vo.setMessage("成功领取 " + ok + " 张优惠券");
        }
        return vo;
    }

    public PromotionCalcVO calcPromotion(PromotionQueryDTO dto) {
        PromotionCalcVO vo = new PromotionCalcVO();
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> coupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, dto.getUserId())
                .eq(UserCoupon::getUseStatus, 0)
                .eq(UserCoupon::getDelFlag, 0)
                .ge(UserCoupon::getValidEnd, now));
        List<UserCouponVO> available = new ArrayList<>();
        BigDecimal maxCoupon = BigDecimal.ZERO;
        for (UserCoupon uc : coupons) {
            CouponTemplate template = templateMapper.selectById(uc.getTemplateId());
            if (template == null) {
                continue;
            }
            BigDecimal fullAmount = template.getFullAmount() == null ? BigDecimal.ZERO : template.getFullAmount();
            if (dto.getOrderAmount().compareTo(fullAmount) < 0) {
                continue;
            }
            UserCouponVO item = new UserCouponVO();
            item.setId(uc.getId());
            item.setTemplateId(uc.getTemplateId());
            item.setCouponName(template.getCouponName());
            item.setFullAmount(template.getFullAmount());
            item.setReduceAmount(template.getReduceAmount());
            item.setValidEnd(uc.getValidEnd());
            available.add(item);
            BigDecimal reduce = template.getReduceAmount() == null ? BigDecimal.ZERO : template.getReduceAmount();
            if (reduce.compareTo(maxCoupon) > 0) {
                maxCoupon = reduce;
            }
        }
        vo.setAvailableCoupons(available);
        vo.setMaxCouponDeduct(maxCoupon);

        FullReduceVO best = findBestFullReduce(dto, now);
        vo.setBestFullReduce(best);
        vo.setMaxFullReduceDeduct(best == null ? BigDecimal.ZERO : best.getReduceAmount());
        return vo;
    }

    /**
     * 我的优惠券列表。
     * status: null/不传=全部；0=未使用；1=已使用；2=已过期
     */
    public List<UserCouponVO> myCoupons(Integer status) {
        Long userId = ContextUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        LocalDateTime now = LocalDateTime.now();
        // 顺带把过期未使用的标记为已过期
        List<UserCoupon> expired = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getUseStatus, 0)
                .eq(UserCoupon::getDelFlag, 0)
                .lt(UserCoupon::getValidEnd, now));
        for (UserCoupon uc : expired) {
            uc.setUseStatus(2);
            userCouponMapper.updateById(uc);
        }

        LambdaQueryWrapper<UserCoupon> qw = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getDelFlag, 0)
                .orderByDesc(UserCoupon::getReceiveTime);
        if (status != null) {
            qw.eq(UserCoupon::getUseStatus, status);
        }
        List<UserCoupon> list = userCouponMapper.selectList(qw);
        List<UserCouponVO> result = new ArrayList<>();
        for (UserCoupon uc : list) {
            CouponTemplate template = templateMapper.selectById(uc.getTemplateId());
            if (template == null) {
                continue;
            }
            UserCouponVO item = new UserCouponVO();
            item.setId(uc.getId());
            item.setTemplateId(uc.getTemplateId());
            item.setCouponName(template.getCouponName());
            item.setFullAmount(template.getFullAmount());
            item.setReduceAmount(template.getReduceAmount());
            item.setValidEnd(uc.getValidEnd());
            item.setUseStatus(uc.getUseStatus());
            result.add(item);
        }
        return result;
    }

    /** 下单核销用户优惠券，返回实际抵扣金额 */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal useCoupon(CouponUseDTO dto) {
        if (dto == null || dto.getUserId() == null || dto.getUserCouponId() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "优惠券参数无效");
        }
        UserCoupon uc = userCouponMapper.selectById(dto.getUserCouponId());
        if (uc == null || (uc.getDelFlag() != null && uc.getDelFlag() == 1)
                || !dto.getUserId().equals(uc.getUserId())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "优惠券不存在");
        }
        if (uc.getUseStatus() == null || uc.getUseStatus() != 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "优惠券已使用或已过期");
        }
        LocalDateTime now = LocalDateTime.now();
        if (uc.getValidEnd() != null && now.isAfter(uc.getValidEnd())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "优惠券已过期");
        }
        CouponTemplate template = templateMapper.selectById(uc.getTemplateId());
        if (template == null || (template.getDelFlag() != null && template.getDelFlag() == 1)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "优惠券模板不存在");
        }
        BigDecimal orderAmount = dto.getOrderAmount() == null ? BigDecimal.ZERO : dto.getOrderAmount();
        if (template.getFullAmount() != null && orderAmount.compareTo(template.getFullAmount()) < 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "未达到优惠券使用门槛");
        }
        BigDecimal reduce = template.getReduceAmount() == null ? BigDecimal.ZERO : template.getReduceAmount();
        if (reduce.compareTo(orderAmount) > 0) {
            reduce = orderAmount;
        }

        uc.setUseStatus(1);
        uc.setOrderNo(dto.getOrderNo());
        userCouponMapper.updateById(uc);

        CouponUseLog log = new CouponUseLog();
        log.setUserCouponId(uc.getId());
        log.setTemplateId(uc.getTemplateId());
        log.setUserId(dto.getUserId());
        log.setOrderNo(dto.getOrderNo());
        log.setDeductMoney(reduce);
        couponUseLogMapper.insert(log);
        return reduce;
    }

    public void warmupSeckillCouponStock() {
        int nextHour = LocalDateTime.now().plusHours(1).getHour();
        LocalDateTime now = LocalDateTime.now();
        List<SeckillCoupon> acts = seckillCouponMapper.selectList(new LambdaQueryWrapper<SeckillCoupon>()
                .eq(SeckillCoupon::getStartHour, nextHour)
                .eq(SeckillCoupon::getStatus, 1)
                .eq(SeckillCoupon::getDelFlag, 0)
                .le(SeckillCoupon::getActivityStart, now)
                .ge(SeckillCoupon::getActivityEnd, now));
        for (SeckillCoupon act : acts) {
            int remain = act.getTotalStock() - act.getUsedNum();
            if (remain > 0) {
                goodsRedisService.setStock(GoodsRedisKeyConstant.SECKILL_COUPON_STOCK + act.getId(), remain);
            }
        }
    }

    public void cleanExpiredCoupons() {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> expired = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUseStatus, 0)
                .lt(UserCoupon::getValidEnd, now));
        for (UserCoupon uc : expired) {
            uc.setUseStatus(2);
            userCouponMapper.updateById(uc);
        }
    }

    // ===== Admin CRUD =====

    public void saveTemplate(CouponTemplate template) {
        if (template.getId() == null) {
            template.setDelFlag(0);
            templateMapper.insert(template);
        } else {
            templateMapper.updateById(template);
        }
    }

    public void deleteTemplate(Long id) {
        CouponTemplate t = templateMapper.selectById(id);
        if (t != null) {
            t.setDelFlag(1);
            templateMapper.updateById(t);
        }
    }

    public List<CouponTemplate> listAllTemplates() {
        return templateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>().eq(CouponTemplate::getDelFlag, 0));
    }

    public void saveFullReduce(FullReduceActivity activity) {
        if (activity.getId() == null) {
            activity.setDelFlag(0);
            fullReduceMapper.insert(activity);
        } else {
            fullReduceMapper.updateById(activity);
        }
        redisUtils.delete(GoodsRedisKeyConstant.PROMOTION_FULLREDUCE_LIST);
    }

    public void deleteFullReduce(Long id) {
        FullReduceActivity a = fullReduceMapper.selectById(id);
        if (a != null) {
            a.setDelFlag(1);
            fullReduceMapper.updateById(a);
        }
        redisUtils.delete(GoodsRedisKeyConstant.PROMOTION_FULLREDUCE_LIST);
    }

    public List<FullReduceActivity> listFullReduce() {
        return fullReduceMapper.selectList(new LambdaQueryWrapper<FullReduceActivity>().eq(FullReduceActivity::getDelFlag, 0));
    }

    public void saveIntegralCoupon(IntegralCoupon config) {
        if (config.getId() == null) {
            config.setDelFlag(0);
            integralCouponMapper.insert(config);
        } else {
            integralCouponMapper.updateById(config);
        }
    }

    public void deleteIntegralCoupon(Long id) {
        IntegralCoupon c = integralCouponMapper.selectById(id);
        if (c != null) {
            c.setDelFlag(1);
            integralCouponMapper.updateById(c);
        }
    }

    public void saveSeckillCoupon(SeckillCoupon act) {
        if (act.getId() == null) {
            act.setDelFlag(0);
            seckillCouponMapper.insert(act);
        } else {
            seckillCouponMapper.updateById(act);
        }
    }

    public void deleteSeckillCoupon(Long id) {
        SeckillCoupon a = seckillCouponMapper.selectById(id);
        if (a != null) {
            a.setDelFlag(1);
            seckillCouponMapper.updateById(a);
        }
    }

    public void saveLotteryPrize(IntegralLotteryPrize prize) {
        if (prize.getId() == null) {
            prize.setDelFlag(0);
            lotteryPrizeMapper.insert(prize);
        } else {
            lotteryPrizeMapper.updateById(prize);
        }
    }

    public void deleteLotteryPrize(Long id) {
        IntegralLotteryPrize p = lotteryPrizeMapper.selectById(id);
        if (p != null) {
            p.setDelFlag(1);
            lotteryPrizeMapper.updateById(p);
        }
    }

    public List<IntegralLotteryPrize> listLotteryPrize() {
        return lotteryPrizeMapper.selectList(new LambdaQueryWrapper<IntegralLotteryPrize>().eq(IntegralLotteryPrize::getDelFlag, 0));
    }

    public PageVO<CouponUseLog> couponLogPage(Integer pageNum, Integer pageSize) {
        Page<CouponUseLog> page = couponUseLogMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<CouponUseLog>().orderByDesc(CouponUseLog::getCreateTime));
        return PageVO.of(page.getTotal(), page.getRecords());
    }

    private UserCoupon createUserCoupon(Long userId, CouponTemplate template) {
        LocalDateTime now = LocalDateTime.now();
        UserCoupon uc = new UserCoupon();
        uc.setTemplateId(template.getId());
        uc.setUserId(userId);
        uc.setReceiveTime(now);
        uc.setValidStart(now);
        uc.setValidEnd(now.plusDays(template.getValidDay()));
        uc.setUseStatus(0);
        uc.setDelFlag(0);
        return uc;
    }

    private void validateTemplate(CouponTemplate template) {
        if (template == null || template.getDelFlag() == 1) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "优惠券不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (template.getStatus() != 1 || now.isBefore(template.getStartTime()) || now.isAfter(template.getEndTime())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "优惠券活动未开始或已结束");
        }
    }

    private int countUserReceived(Long userId, Long templateId) {
        String limitKey = GoodsRedisKeyConstant.COUPON_USER_LIMIT + userId + ":" + templateId;
        Integer cached = goodsRedisService.getUserLimit(limitKey);
        if (cached > 0) {
            return cached;
        }
        long count = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getTemplateId, templateId)
                .eq(UserCoupon::getDelFlag, 0));
        return (int) count;
    }

    private int countReceived(Long templateId) {
        long count = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getTemplateId, templateId)
                .eq(UserCoupon::getDelFlag, 0));
        return (int) count;
    }

    private CouponTemplateVO toTemplateVO(CouponTemplate t) {
        CouponTemplateVO vo = new CouponTemplateVO();
        BeanUtils.copyProperties(t, vo);
        vo.setRemainCount(t.getTotalCount() - countReceived(t.getId()));
        return vo;
    }

    private FullReduceVO findBestFullReduce(PromotionQueryDTO dto, LocalDateTime now) {
        String cache = redisUtils.get(GoodsRedisKeyConstant.PROMOTION_FULLREDUCE_LIST);
        List<FullReduceActivity> activities;
        if (cache != null) {
            activities = com.alibaba.fastjson2.JSON.parseArray(cache, FullReduceActivity.class);
        } else {
            activities = fullReduceMapper.selectList(new LambdaQueryWrapper<FullReduceActivity>()
                    .eq(FullReduceActivity::getStatus, 1)
                    .eq(FullReduceActivity::getDelFlag, 0)
                    .le(FullReduceActivity::getStartTime, now)
                    .ge(FullReduceActivity::getEndTime, now));
            redisUtils.set(GoodsRedisKeyConstant.PROMOTION_FULLREDUCE_LIST, JsonUtils.toJson(activities),
                    properties.getCouponTemplateTtl(), TimeUnit.SECONDS);
        }
        FullReduceActivity best = null;
        for (FullReduceActivity act : activities) {
            if (dto.getOrderAmount().compareTo(act.getFullAmount()) < 0) {
                continue;
            }
            if (act.getTargetType() == 2 && dto.getCatIds() != null) {
                Set<Long> targetCats = Arrays.stream(act.getTargetCatIds().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).collect(Collectors.toSet());
                boolean match = dto.getCatIds().stream().anyMatch(targetCats::contains);
                if (!match) {
                    continue;
                }
            }
            if (best == null || act.getReduceAmount().compareTo(best.getReduceAmount()) > 0) {
                best = act;
            }
        }
        if (best == null) {
            return null;
        }
        FullReduceVO vo = new FullReduceVO();
        BeanUtils.copyProperties(best, vo);
        return vo;
    }

    private Long requireUserId() {
        Long userId = ContextUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }

    private IntegralCouponVO toIntegralVO(IntegralCoupon c) {
        IntegralCouponVO vo = new IntegralCouponVO();
        BeanUtils.copyProperties(c, vo);
        int used = c.getUsedNum() == null ? 0 : c.getUsedNum();
        int total = c.getTotalStock() == null ? 0 : c.getTotalStock();
        vo.setRemainStock(Math.max(0, total - used));
        CouponTemplate t = templateMapper.selectById(c.getTemplateId());
        if (t != null) {
            vo.setCouponName(t.getCouponName());
            vo.setCouponType(t.getCouponType());
            vo.setFullAmount(t.getFullAmount());
            vo.setReduceAmount(t.getReduceAmount());
        }
        return vo;
    }

    private SeckillCouponVO toSeckillVO(SeckillCoupon act) {
        SeckillCouponVO vo = new SeckillCouponVO();
        BeanUtils.copyProperties(act, vo);
        int used = act.getUsedNum() == null ? 0 : act.getUsedNum();
        int total = act.getTotalStock() == null ? 0 : act.getTotalStock();
        vo.setRemainStock(Math.max(0, total - used));
        CouponTemplate t = templateMapper.selectById(act.getTemplateId());
        if (t != null) {
            vo.setCouponName(t.getCouponName());
            vo.setCouponType(t.getCouponType());
            vo.setFullAmount(t.getFullAmount());
            vo.setReduceAmount(t.getReduceAmount());
        }
        LocalDateTime now = LocalDateTime.now();
        boolean inWindow = !now.isBefore(act.getActivityStart()) && !now.isAfter(act.getActivityEnd());
        if (!inWindow || act.getStatus() == null || act.getStatus() != 1) {
            vo.setGrabStatus(2);
            vo.setGrabStatusText("已结束");
        } else if (now.getHour() == act.getStartHour()) {
            vo.setGrabStatus(1);
            vo.setGrabStatusText("抢购中");
        } else {
            vo.setGrabStatus(0);
            vo.setGrabStatusText(String.format("每日 %02d:00 开抢", act.getStartHour()));
        }
        return vo;
    }

    private LotteryPrizeVO toLotteryPrizeVO(IntegralLotteryPrize p) {
        LotteryPrizeVO vo = new LotteryPrizeVO();
        BeanUtils.copyProperties(p, vo);
        if (p.getRewardType() != null && p.getRewardType() == 2 && p.getRewardCouponId() != null) {
            CouponTemplate t = templateMapper.selectById(p.getRewardCouponId());
            if (t != null) {
                vo.setRewardName(t.getCouponName());
            }
        } else if (p.getRewardType() != null && p.getRewardType() == 1) {
            vo.setRewardName((p.getRewardIntegral() == null ? 0 : p.getRewardIntegral()) + "积分");
        }
        return vo;
    }

    private IntegralLotteryPrize weightedPick(List<IntegralLotteryPrize> prizes) {
        int total = prizes.stream().mapToInt(p -> p.getWeight() == null ? 0 : Math.max(0, p.getWeight())).sum();
        if (total <= 0) {
            return prizes.get(0);
        }
        int r = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (IntegralLotteryPrize p : prizes) {
            acc += p.getWeight() == null ? 0 : Math.max(0, p.getWeight());
            if (r < acc) {
                return p;
            }
        }
        return prizes.get(prizes.size() - 1);
    }

    private void deductUserIntegral(Long userId, int cost, String remark) {
        UserIntegralDTO dto = new UserIntegralDTO();
        dto.setUserId(userId);
        dto.setIntegral(cost);
        dto.setRemark(remark);
        Result<Void> result;
        try {
            result = userFeignClient.deductIntegral(dto);
        } catch (Exception e) {
            log.warn("扣积分 Feign 失败 userId={} cost={}", userId, cost, e);
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "积分服务暂不可用");
        }
        if (result == null || result.getCode() == null || result.getCode() != 200) {
            String msg = result != null && result.getMsg() != null ? result.getMsg() : "积分不足";
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), msg);
        }
    }

    private void refundUserIntegral(Long userId, int amount, String remark) {
        changeUserIntegral(userId, amount, remark);
    }

    private void changeUserIntegral(Long userId, int amount, String remark) {
        UserIntegralDTO dto = new UserIntegralDTO();
        dto.setUserId(userId);
        dto.setIntegral(amount);
        dto.setRemark(remark);
        try {
            Result<Void> result = userFeignClient.changeIntegral(dto);
            if (result == null || result.getCode() == null || result.getCode() != 200) {
                log.warn("积分变更失败 userId={} amount={} msg={}", userId,
                        amount, result == null ? null : result.getMsg());
            }
        } catch (Exception e) {
            log.warn("积分变更 Feign 失败 userId={} amount={}", userId, amount, e);
        }
    }
}
