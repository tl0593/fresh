package com.fresh.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fresh.user.entity.AppUser;
import com.fresh.user.mapper.AppUserMapper;
import com.fresh.user.vo.UserDailyStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserStatService {

    private final AppUserMapper appUserMapper;

    public UserDailyStatVO dailyStat(LocalDate statDate) {
        LocalDate date = statDate == null ? LocalDate.now() : statDate;
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        Long newUser = appUserMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getDelFlag, 0)
                .ge(AppUser::getCreateTime, start)
                .lt(AppUser::getCreateTime, end));

        // 活跃：当日有更新的用户（登录/资料变更等）
        Long activeUser = appUserMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getDelFlag, 0)
                .ge(AppUser::getUpdateTime, start)
                .lt(AppUser::getUpdateTime, end));

        UserDailyStatVO vo = new UserDailyStatVO();
        vo.setNewUser(newUser == null ? 0 : newUser.intValue());
        vo.setActiveUser(activeUser == null ? 0 : activeUser.intValue());
        return vo;
    }
}
