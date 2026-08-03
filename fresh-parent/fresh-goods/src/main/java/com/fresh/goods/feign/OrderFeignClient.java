package com.fresh.goods.feign;

import com.fresh.common.base.Result;
import com.fresh.goods.dto.OrderItemCheckVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "fresh-order", url = "${fresh.feign.order-url:}")
public interface OrderFeignClient {

    @GetMapping("/feign/comment/check/{orderItemId}")
    Result<OrderItemCheckVO> checkCanComment(@PathVariable("orderItemId") Long orderItemId);

    @PutMapping("/feign/comment/mark/{orderItemId}")
    Result<Void> markCommented(@PathVariable("orderItemId") Long orderItemId);
}
