package com.fresh.goods.entity.comment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("goods_comment")
public class GoodsComment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long orderItemId;
    private String orderNo;
    private Long goodsId;
    private Long specId;
    private Integer score;
    private String content;
    private Integer status;
    private Integer delFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
