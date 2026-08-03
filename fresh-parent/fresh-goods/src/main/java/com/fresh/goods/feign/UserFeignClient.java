package com.fresh.goods.feign;

import com.fresh.common.base.Result;
import com.fresh.goods.dto.UserIntegralDTO;
import com.fresh.goods.vo.AppUserSimpleVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "fresh-user", url = "${fresh.feign.user-url:}")
public interface UserFeignClient {

    @GetMapping("/feign/user/{userId}")
    Result<AppUserSimpleVO> getUserById(@PathVariable("userId") Long userId);

    @PostMapping("/feign/integral/deduct")
    Result<Void> deductIntegral(@RequestBody UserIntegralDTO dto);

    @PostMapping("/feign/integral/change")
    Result<Void> changeIntegral(@RequestBody UserIntegralDTO dto);
}
