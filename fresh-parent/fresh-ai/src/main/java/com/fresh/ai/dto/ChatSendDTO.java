package com.fresh.ai.dto;

import lombok.Data;

@Data
public class ChatSendDTO {

    private String sessionKey;
    private String userMsg;
    private Integer chatType;
}
