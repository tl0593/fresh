package com.fresh.data.feign;

import com.fresh.common.base.Result;
import com.fresh.data.dto.UserStatDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "fresh-user", url = "${fresh.feign.user-url:}", contextId = "userStatFeignClient")
public interface UserStatFeignClient {

    @GetMapping("/feign/user/dailyStat")
    Result<UserStatDTO> dailyStat(@RequestParam("statDate") String statDate);
}
