package com.fresh.goods.entity.comment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment_image")
public class CommentImage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private String imgUrl;
    private Integer sort;
    private LocalDateTime createTime;
}
