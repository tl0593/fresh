package com.fresh.goods.entity.comment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment_reply")
public class CommentReply {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private Long adminId;
    private String replyContent;
    private Integer delFlag;
    private LocalDateTime createTime;
}
