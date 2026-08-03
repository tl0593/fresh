package com.fresh.goods.entity.promotion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_coupon")
public class UserCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long userId;
    private LocalDateTime receiveTime;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private Integer useStatus;
    private String orderNo;
    private Integer delFlag;
}
