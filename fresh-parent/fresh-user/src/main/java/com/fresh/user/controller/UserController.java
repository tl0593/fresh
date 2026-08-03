package com.fresh.user.controller;

import com.fresh.common.base.Result;
import com.fresh.common.util.ContextUtil;
import com.fresh.user.dto.AdminLoginDTO;
import com.fresh.user.dto.CartUpdateDTO;
import com.fresh.user.dto.MiniLoginDTO;
import com.fresh.user.entity.UserAddress;
import com.fresh.user.entity.UserIntegralLog;
import com.fresh.user.service.AddressService;
import com.fresh.user.service.AuthService;
import com.fresh.user.service.CartService;
import com.fresh.user.service.IntegralService;
import com.fresh.user.vo.AppUserVO;
import com.fresh.user.vo.CartItemVO;
import com.fresh.user.vo.LoginVO;
import com.fresh.user.vo.UserAddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final AddressService addressService;
    private final CartService cartService;
    private final IntegralService integralService;

    @PostMapping("/mini/login")
    public Result<LoginVO> miniLogin(@RequestBody MiniLoginDTO dto) {
        return Result.success(authService.miniLogin(dto));
    }

    @PostMapping("/admin/login")
    public Result<LoginVO> adminLogin(@RequestBody AdminLoginDTO dto) {
        return Result.success(authService.adminLogin(dto));
    }

    @GetMapping("/address/list")
    public Result<List<UserAddressVO>> addressList() {
        return Result.success(addressService.list());
    }

    @PostMapping("/address/save")
    public Result<Void> saveAddress(@RequestBody UserAddress address) {
        addressService.save(address);
        return Result.success();
    }

    @GetMapping("/cart/list")
    public Result<List<CartItemVO>> cartList() {
        return Result.success(cartService.list());
    }

    @PostMapping("/cart/update")
    public Result<Void> cartUpdate(@RequestBody CartUpdateDTO dto) {
        cartService.update(dto);
        return Result.success();
    }

    @GetMapping("/integral/log")
    public Result<List<UserIntegralLog>> integralLog() {
        return Result.success(integralService.logs(ContextUtil.getUserId()));
    }

    /** 当前用户积分余额 */
    @GetMapping("/integral/balance")
    public Result<AppUserVO> integralBalance() {
        return Result.success(integralService.getUserById(ContextUtil.getUserId()));
    }
}
