package com.fresh.data.feign;

import com.fresh.common.base.Result;
import com.fresh.data.dto.GoodsSalesItemDTO;
import com.fresh.data.dto.StatQueryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "fresh-order", url = "${fresh.feign.order-url:}", contextId = "goodsStatFeignClient")
public interface GoodsStatFeignClient {

    @PostMapping("/feign/order/goodsSalesStat")
    Result<List<GoodsSalesItemDTO>> dailySalesStat(@RequestBody StatQueryDTO dto);
}
