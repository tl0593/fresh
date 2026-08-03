package com.fresh.ai.vo;

import lombok.Data;

@Data
public class ChatReplyVO {

    private Long recordId;
    private String sessionKey;
    private String aiReply;
}
