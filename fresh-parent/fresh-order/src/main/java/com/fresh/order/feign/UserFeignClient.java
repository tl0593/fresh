package com.fresh.order.feign;

import com.fresh.common.base.Result;
import com.fresh.order.dto.IntegralDTO;
import com.fresh.order.dto.StockChangeDTO;
import com.fresh.order.vo.AppUserVO;
import com.fresh.order.vo.UserAddressVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "fresh-user", url = "${fresh.feign.user-url:}")
public interface UserFeignClient {

    @GetMapping("/feign/user/{userId}")
    Result<AppUserVO> getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/feign/address/{addressId}")
    Result<UserAddressVO> getAddressById(@PathVariable("addressId") Long addressId);

    @PostMapping("/feign/integral/freeze")
    Result<Void> freezeIntegral(@RequestBody IntegralDTO dto);

    @PostMapping("/feign/integral/unfreeze")
    Result<Void> unfreezeIntegral(@RequestBody IntegralDTO dto);

    @PostMapping("/feign/integral/change")
    Result<Void> changeIntegral(@RequestBody IntegralDTO dto);
}
