package com.fresh.goods.dto;

import lombok.Data;

@Data
public class CommentReplyDTO {

    private Long commentId;
    private Long adminId;
    private String replyContent;
}
