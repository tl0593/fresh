package com.fresh.data.dto;

import lombok.Data;

@Data
public class AiChatBehaviorMqDTO {

    private Long userId;
    private String sessionKey;
    private Integer chatType;
}
