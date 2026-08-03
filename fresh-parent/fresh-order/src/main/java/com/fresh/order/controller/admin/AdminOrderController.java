package com.fresh.order.controller.admin;

import com.fresh.common.base.Result;
import com.fresh.order.dto.AfterSaleAuditDTO;
import com.fresh.order.entity.OrderMain;
import com.fresh.order.service.OrderService;
import com.fresh.order.vo.AfterSaleAdminVO;
import com.fresh.order.vo.OrderDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping("/admin/order/list")
    public Result<List<OrderMain>> list(@RequestParam(required = false) Integer status) {
        return Result.success(orderService.listAllForAdmin(status));
    }

    @GetMapping("/admin/order/{orderNo}")
    public Result<OrderDetailVO> detail(@PathVariable String orderNo) {
        return Result.success(orderService.adminDetail(orderNo));
    }

    /** 核销自提：待自提 → 已完成 */
    @PostMapping("/admin/order/{orderNo}/complete")
    public Result<Void> complete(@PathVariable String orderNo) {
        orderService.completePickup(orderNo);
        return Result.success();
    }

    /** 配送到站：待配送 → 待自提 */
    @PostMapping("/admin/order/{orderNo}/arrive")
    public Result<Void> arrive(@PathVariable String orderNo) {
        orderService.markArrived(orderNo);
        return Result.success();
    }

    /** 售后工单分页/列表 */
    @GetMapping("/admin/afterSale/page")
    public Result<List<AfterSaleAdminVO>> afterSalePage(@RequestParam(required = false) Integer auditStatus) {
        return Result.success(orderService.listAfterSaleForAdmin(auditStatus));
    }

    /** 待审核售后数量（菜单红点） */
    @GetMapping("/admin/afterSale/pendingCount")
    public Result<Map<String, Long>> pendingAfterSaleCount() {
        Map<String, Long> data = new HashMap<>(1);
        data.put("count", orderService.countPendingAfterSale());
        return Result.success(data);
    }

    /** 售后审核：通过/驳回 */
    @PostMapping("/admin/afterSale/audit")
    public Result<Void> auditAfterSale(@RequestBody AfterSaleAuditDTO dto) {
        orderService.auditAfterSale(dto);
        return Result.success();
    }
}
