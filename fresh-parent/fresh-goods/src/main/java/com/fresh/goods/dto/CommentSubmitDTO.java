package com.fresh.goods.dto;

import lombok.Data;

import java.util.List;

@Data
public class CommentSubmitDTO {

    private Long orderItemId;
    private Integer score;
    private String content;
    private List<String> images;
}
