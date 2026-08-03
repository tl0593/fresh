package com.fresh.user.vo;

import lombok.Data;

@Data
public class LoginVO {

    private String token;
    private AppUserVO userInfo;
}
