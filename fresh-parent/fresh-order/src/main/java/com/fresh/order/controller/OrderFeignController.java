package com.fresh.order.controller;

import com.fresh.common.base.Result;
import com.fresh.order.dto.AfterSaleAiResultDTO;
import com.fresh.order.dto.GoodsSalesItemVO;
import com.fresh.order.dto.OrderItemCheckVO;
import com.fresh.order.dto.OrderStatVO;
import com.fresh.order.dto.StatQueryDTO;
import com.fresh.order.entity.OrderMain;
import com.fresh.order.service.OrderService;
import com.fresh.order.service.OrderStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/feign")
@RequiredArgsConstructor
public class OrderFeignController {

    private final OrderService orderService;
    private final OrderStatService orderStatService;

    @GetMapping("/order/{orderNo}")
    public Result<OrderMain> getOrderByNo(@PathVariable("orderNo") String orderNo) {
        return Result.success(orderService.getByOrderNo(orderNo));
    }

    @PutMapping("/afterSale/aiResult")
    public Result<Void> updateAfterSaleAiResult(@RequestBody AfterSaleAiResultDTO dto) {
        orderService.updateAfterSaleAiResult(dto);
        return Result.success();
    }

    @PostMapping("/order/batchStat")
    public Result<OrderStatVO> batchStat(@RequestBody StatQueryDTO dto) {
        return Result.success(orderStatService.stat(dto));
    }

    @PostMapping("/order/goodsSalesStat")
    public Result<List<GoodsSalesItemVO>> goodsSalesStat(@RequestBody StatQueryDTO dto) {
        return Result.success(orderStatService.goodsSalesStat(dto));
    }

    @GetMapping("/comment/check/{orderItemId}")
    public Result<OrderItemCheckVO> checkCanComment(@PathVariable("orderItemId") Long orderItemId) {
        return Result.success(orderService.checkCanComment(orderItemId));
    }

    @PutMapping("/comment/mark/{orderItemId}")
    public Result<Void> markCommented(@PathVariable("orderItemId") Long orderItemId) {
        orderService.markOrderItemCommented(orderItemId);
        return Result.success();
    }
}
