package com.fresh.goods.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {

    private Long id;
    private Long userId;
    private Long orderItemId;
    private String orderNo;
    private Long goodsId;
    private Long specId;
    private Integer score;
    private String content;
    private Integer status;
    private LocalDateTime createTime;
    private List<String> images;
    private CommentReplyVO reply;
}
