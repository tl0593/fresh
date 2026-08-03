package com.fresh.message.feign;

import com.fresh.common.base.Result;
import com.fresh.message.vo.FeignOrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "fresh-order", contextId = "messageOrderFeignClient", url = "${spring.cloud.openfeign.client.config.fresh-order.url:}")
public interface OrderFeignClient {

    @GetMapping("/feign/order/{orderNo}")
    Result<FeignOrderVO> getOrderByNo(@PathVariable("orderNo") String orderNo);
}
