package com.fresh.user.controller;

import com.fresh.common.base.Result;
import com.fresh.user.dto.IntegralDTO;
import com.fresh.user.service.AddressService;
import com.fresh.user.service.IntegralService;
import com.fresh.user.service.UserStatService;
import com.fresh.user.vo.AppUserVO;
import com.fresh.user.vo.UserAddressVO;
import com.fresh.user.vo.UserDailyStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/feign")
@RequiredArgsConstructor
public class UserFeignController {

    private final IntegralService integralService;
    private final AddressService addressService;
    private final UserStatService userStatService;

    @GetMapping("/user/{userId}")
    public Result<AppUserVO> getUserById(@PathVariable("userId") Long userId) {
        return Result.success(integralService.getUserById(userId));
    }

    @GetMapping("/address/{addressId}")
    public Result<UserAddressVO> getAddressById(@PathVariable("addressId") Long addressId) {
        return Result.success(addressService.getById(addressId));
    }

    @PostMapping("/integral/freeze")
    public Result<Void> freezeIntegral(@RequestBody IntegralDTO dto) {
        integralService.freeze(dto);
        return Result.success();
    }

    @PostMapping("/integral/unfreeze")
    public Result<Void> unfreezeIntegral(@RequestBody IntegralDTO dto) {
        integralService.unfreeze(dto);
        return Result.success();
    }

    @PostMapping("/integral/change")
    public Result<Void> changeIntegral(@RequestBody IntegralDTO dto) {
        integralService.change(dto);
        return Result.success();
    }

    /** 扣减可用积分（兑券/抽奖） */
    @PostMapping("/integral/deduct")
    public Result<Void> deductIntegral(@RequestBody IntegralDTO dto) {
        integralService.deduct(dto);
        return Result.success();
    }

    @GetMapping("/user/dailyStat")
    public Result<UserDailyStatVO> dailyStat(@RequestParam("statDate") String statDate) {
        return Result.success(userStatService.dailyStat(LocalDate.parse(statDate)));
    }
}
