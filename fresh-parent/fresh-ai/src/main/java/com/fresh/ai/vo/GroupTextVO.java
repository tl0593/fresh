package com.fresh.ai.vo;

import lombok.Data;

import java.util.List;

@Data
public class GroupTextVO {

    private List<String> texts;
    private Long logId;
}
