package com.fresh.user.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fresh.common.constant.RedisKeyConstant;
import com.fresh.common.dto.UserContextDTO;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.fresh.common.util.CryptoUtils;
import com.fresh.common.util.IdUtils;
import com.fresh.common.util.JsonUtils;
import com.fresh.common.util.RedisUtils;
import com.fresh.common.util.WechatUtil;
import com.fresh.user.config.UserProperties;
import com.fresh.user.dto.AdminLoginDTO;
import com.fresh.user.dto.MiniLoginDTO;
import com.fresh.user.entity.AppUser;
import com.fresh.user.entity.SysAdmin;
import com.fresh.user.mapper.AppUserMapper;
import com.fresh.user.mapper.SysAdminMapper;
import com.fresh.user.vo.AppUserVO;
import com.fresh.user.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserMapper appUserMapper;
    private final SysAdminMapper sysAdminMapper;
    private final RedisUtils redisUtils;
    private final UserProperties userProperties;
    private final WechatUtil wechatUtil;
    private final UserMqProducer userMqProducer;

    @SentinelResource("miniLogin")
    public LoginVO miniLogin(MiniLoginDTO dto) {
        if (StrUtil.isBlank(dto.getCode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST);
        }
        String openid = wechatUtil.code2Session(dto.getCode());

        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getOpenid, openid));
        boolean isNewUser = false;
        if (user == null) {
            isNewUser = true;
            user = new AppUser();
            user.setOpenid(openid);
            user.setNickName("生鲜用户" + IdUtils.nextIdStr().substring(10));
            user.setAvatar("");
            user.setIntegral(0);
            user.setFrozenIntegral(0);
            user.setStatus(1);
            user.setDelFlag(0);
            appUserMapper.insert(user);
        }

        String token = IdUtils.nextIdStr();
        UserContextDTO context = new UserContextDTO();
        context.setUserId(user.getId());
        context.setOpenid(user.getOpenid());
        context.setRoleType(1);
        redisUtils.set(RedisKeyConstant.USER_TOKEN + token,
                JsonUtils.toJson(context),
                userProperties.getMiniTokenExpire(),
                TimeUnit.SECONDS);

        if (isNewUser) {
            userMqProducer.sendUserRegister(user.getId(), openid);
            userMqProducer.sendUserBehavior(user.getId(), 1, null);
        }

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserInfo(toUserVO(user));
        return vo;
    }

    public LoginVO adminLogin(AdminLoginDTO dto) {
        SysAdmin admin = sysAdminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getUsername, dto.getUsername())
                .eq(SysAdmin::getDelFlag, 0));
        if (admin == null || !CryptoUtils.matchPassword(dto.getPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED.getCode(), "账号或密码错误");
        }

        String token = IdUtils.nextIdStr();
        UserContextDTO context = new UserContextDTO();
        context.setAdminId(admin.getId());
        context.setRoleType(2);
        redisUtils.set(RedisKeyConstant.USER_TOKEN + token,
                JsonUtils.toJson(context),
                userProperties.getAdminTokenExpire(),
                TimeUnit.SECONDS);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        AppUserVO info = new AppUserVO();
        info.setId(admin.getId());
        info.setNickName(admin.getRealName());
        vo.setUserInfo(info);
        return vo;
    }

    public AppUserVO toUserVO(AppUser user) {
        AppUserVO vo = new AppUserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
