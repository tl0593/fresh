package com.fresh.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("goods_category")
public class GoodsCategory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String catName;
    private String icon;
    private Integer sort;
    private Integer status;
    private Integer delFlag;
    private LocalDateTime createTime;
}
