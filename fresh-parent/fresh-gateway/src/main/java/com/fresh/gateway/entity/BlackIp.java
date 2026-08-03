package com.fresh.gateway.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("black_ip")
public class BlackIp {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String ip;
    private String reason;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
