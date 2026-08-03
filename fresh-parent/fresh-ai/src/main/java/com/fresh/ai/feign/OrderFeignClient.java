package com.fresh.ai.feign;

import com.fresh.ai.dto.AfterSaleAiResultDTO;
import com.fresh.common.base.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "fresh-order")
public interface OrderFeignClient {

    @PutMapping("/feign/afterSale/aiResult")
    Result<Void> updateAfterSaleAiResult(@RequestBody AfterSaleAiResultDTO dto);
}
