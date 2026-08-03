package com.fresh.gateway.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("gateway_access_log")
public class GatewayAccessLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestIp;
    private String requestUrl;
    private String requestMethod;
    private Long userId;
    private String token;
    private Integer responseCode;
    private Integer costTime;
    private LocalDateTime createTime;
}
