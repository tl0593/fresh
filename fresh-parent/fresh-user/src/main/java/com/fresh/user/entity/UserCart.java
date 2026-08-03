package com.fresh.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_cart")
public class UserCart {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long goodsId;
    private Long specId;
    private Integer num;
    private Integer selected;
    private Integer delFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
