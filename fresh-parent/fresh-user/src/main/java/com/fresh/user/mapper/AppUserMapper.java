package com.fresh.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fresh.user.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
}
