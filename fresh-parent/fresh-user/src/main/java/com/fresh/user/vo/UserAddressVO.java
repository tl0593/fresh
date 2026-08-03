package com.fresh.user.vo;

import lombok.Data;

@Data
public class UserAddressVO {

    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String community;
    private String detailAddr;
    private Integer isDefault;
}
