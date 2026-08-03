package com.fresh.ai.vo;

import lombok.Data;

@Data
public class CookHistoryVO {

    private Long userId;
    private String content;
    private Long cacheTime;
}
