package com.fresh.gateway.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("limit_config")
public class LimitConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String apiPath;
    private Integer limitType;
    private Integer limitCount;
    private Integer timeSecond;
    private Integer status;
    private LocalDateTime createTime;
}
