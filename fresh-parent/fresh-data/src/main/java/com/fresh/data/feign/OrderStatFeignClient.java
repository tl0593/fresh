package com.fresh.data.feign;

import com.fresh.common.base.Result;
import com.fresh.data.dto.OrderStatDTO;
import com.fresh.data.dto.StatQueryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "fresh-order", url = "${fresh.feign.order-url:}", contextId = "orderStatFeignClient")
public interface OrderStatFeignClient {

    @PostMapping("/feign/order/batchStat")
    Result<OrderStatDTO> batchStat(@RequestBody StatQueryDTO dto);
}
