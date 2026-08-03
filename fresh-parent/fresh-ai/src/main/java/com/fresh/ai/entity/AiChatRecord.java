package com.fresh.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_chat_record")
public class AiChatRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String sessionKey;
    private String userMsg;
    private String aiReply;
    private Integer chatType;
    private LocalDateTime createTime;
}
