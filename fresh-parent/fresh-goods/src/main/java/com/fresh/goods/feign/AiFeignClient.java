package com.fresh.goods.feign;

import com.fresh.goods.dto.GroupTextRequestDTO;
import com.fresh.common.base.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "fresh-ai", url = "${fresh.feign.ai-url:}")
public interface AiFeignClient {

    @PostMapping("/feign/group/text")
    Result<String> generateGroupText(@RequestBody GroupTextRequestDTO dto);
}
