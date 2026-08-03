package com.fresh.goods.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentReplyVO {

    private Long id;
    private String replyContent;
    private LocalDateTime createTime;
}
