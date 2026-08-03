package com.fresh.message.feign;

import com.fresh.common.base.Result;
import com.fresh.message.vo.FeignUserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "fresh-user", contextId = "messageUserFeignClient", url = "${spring.cloud.openfeign.client.config.fresh-user.url:}")
public interface UserFeignClient {

    @GetMapping("/feign/user/{userId}")
    Result<FeignUserVO> getUserById(@PathVariable("userId") Long userId);
}
