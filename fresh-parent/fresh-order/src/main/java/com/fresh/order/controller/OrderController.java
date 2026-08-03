package com.fresh.order.controller;

import com.fresh.common.base.Result;
import com.fresh.order.dto.OrderCreateDTO;
import com.fresh.order.dto.OrderSettleVO;
import com.fresh.order.entity.AfterSale;
import com.fresh.order.service.OrderService;
import com.fresh.order.service.WxPayService;
import com.fresh.order.vo.AfterSaleAdminVO;
import com.fresh.order.vo.OrderDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final WxPayService wxPayService;

    /** 结算预览：服务端按规格/活动价计算应付金额 */
    @PostMapping("/order/settle")
    public Result<OrderSettleVO> settle(@RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.settlePreview(dto));
    }

    @PostMapping("/order/create")
    public Result<Map<String, String>> create(@RequestBody OrderCreateDTO dto) {
        String orderNo = orderService.createOrder(dto);
        return Result.success(Map.of("orderNo", orderNo));
    }

    @PostMapping("/order/pay/prepay")
    public Result<Map<String, String>> prepay(@RequestBody Map<String, String> body) {
        return Result.success(wxPayService.createPrepay(body.get("orderNo")));
    }

    @GetMapping("/order/list")
    public Result<List<OrderDetailVO>> list() {
        return Result.success(orderService.listByUserWithItems());
    }

    @GetMapping("/order/{orderNo}")
    public Result<OrderDetailVO> detail(@PathVariable String orderNo) {
        return Result.success(orderService.detailWithItems(orderNo));
    }

    /** mock / 开发环境模拟支付成功（需登录；仅 wx-pay-mock=true） */
    @PostMapping("/order/pay/callback")
    public Result<Void> payCallback(@RequestBody Map<String, String> body) {
        wxPayService.handleMockCallback(body);
        return Result.success();
    }

    /** 微信异步通知入口（网关白名单；正式环境需验签） */
    @PostMapping("/order/pay/notify")
    public Result<Void> payNotify(@RequestBody Map<String, String> body) {
        wxPayService.handleNotify(body);
        return Result.success();
    }

    @PostMapping("/afterSale/apply")
    public Result<Void> applyAfterSale(@RequestBody AfterSale afterSale) {
        orderService.applyAfterSale(afterSale);
        return Result.success();
    }

    /** 用户端：我的售后列表 */
    @GetMapping("/afterSale/mine")
    public Result<List<AfterSaleAdminVO>> myAfterSales() {
        return Result.success(orderService.listAfterSaleForUser());
    }
}
