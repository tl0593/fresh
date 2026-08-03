package com.fresh.user.vo;

import lombok.Data;

@Data
public class AppUserVO {

    private Long id;
    private String openid;
    private String nickName;
    private String avatar;
    private String phone;
    private Integer integral;
}
