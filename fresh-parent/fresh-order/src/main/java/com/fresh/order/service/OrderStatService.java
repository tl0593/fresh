package com.fresh.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fresh.common.constant.OrderConstant;
import com.fresh.order.dto.GoodsSalesItemVO;
import com.fresh.order.dto.OrderStatVO;
import com.fresh.order.dto.StatQueryDTO;
import com.fresh.order.entity.AfterSale;
import com.fresh.order.entity.OrderMain;
import com.fresh.order.mapper.AfterSaleMapper;
import com.fresh.order.mapper.OrderItemMapper;
import com.fresh.order.mapper.OrderMainMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderStatService {

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final AfterSaleMapper afterSaleMapper;

    public OrderStatVO stat(StatQueryDTO query) {
        LocalDate date = query.getStartDate() != null ? query.getStartDate() : LocalDate.now();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        // 已支付：待配送 / 待自提 / 已完成 / 售后中
        List<OrderMain> paidOrders = orderMainMapper.selectList(new LambdaQueryWrapper<OrderMain>()
                .eq(OrderMain::getDelFlag, 0)
                .in(OrderMain::getStatus,
                        OrderConstant.STATUS_WAIT_DELIVERY,
                        OrderConstant.STATUS_WAIT_PICKUP,
                        OrderConstant.STATUS_COMPLETED,
                        OrderConstant.STATUS_AFTER_SALE)
                .ge(OrderMain::getPayTime, start)
                .lt(OrderMain::getPayTime, end));

        OrderStatVO vo = new OrderStatVO();
        vo.setOrderCount(paidOrders.size());
        vo.setOrderAmount(paidOrders.stream()
                .map(o -> o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setGroupSuccessNum((int) paidOrders.stream()
                .filter(o -> o.getGroupRecordId() != null)
                .count());

        Long afterSaleCount = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getDelFlag, 0)
                .ge(AfterSale::getCreateTime, start)
                .lt(AfterSale::getCreateTime, end));
        vo.setAfterSaleNum(afterSaleCount == null ? 0 : afterSaleCount.intValue());
        return vo;
    }

    public List<GoodsSalesItemVO> goodsSalesStat(StatQueryDTO query) {
        LocalDate date = query.getStartDate() != null ? query.getStartDate() : LocalDate.now();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<GoodsSalesItemVO> list = orderItemMapper.aggregatePaidSales(start, end);
        return list == null ? List.of() : list;
    }
}
