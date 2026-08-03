package com.fresh.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_user")
public class AppUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String nickName;
    private String avatar;
    private String phone;
    private Integer integral;
    private Integer frozenIntegral;
    private Integer status;
    private Integer delFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
