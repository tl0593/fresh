package com.fresh.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserContextDTO implements Serializable {

    private Long userId;
    private Long adminId;
    private String openid;
    /** 1=小程序用户 2=管理员 */
    private Integer roleType;
}
