package com.fresh.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_integral_log")
public class UserIntegralLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer changeNum;
    private Integer type;
    private Long orderId;
    private String remark;
    private LocalDateTime createTime;
}
