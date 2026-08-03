package com.fresh.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("msg_template")
public class MsgTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer templateType;
    private String templateId;
    private String title;
    private String content;
    private Integer status;
    private LocalDateTime createTime;
}
