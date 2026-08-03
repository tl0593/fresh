package com.fresh.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("msg_send_log")
public class MsgSendLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long templateId;
    private Integer businessType;
    private Long businessId;
    private String sendContent;
    private String phone;
    private String openid;
    private Integer sendStatus;
    private String errMsg;
    private LocalDateTime createTime;
}
