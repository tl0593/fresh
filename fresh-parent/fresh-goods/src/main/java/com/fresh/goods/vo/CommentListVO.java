package com.fresh.goods.vo;

import lombok.Data;

import java.util.List;

@Data
public class CommentListVO {

    private List<CommentVO> records;
    private Long total;
    private Double avgScore;
    private Double goodRate;
}
