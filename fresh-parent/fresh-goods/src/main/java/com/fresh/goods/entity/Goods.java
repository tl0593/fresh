package com.fresh.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("goods")
public class Goods {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long catId;
    private String goodsName;
    private String goodsImg;
    private String goodsDesc;
    private BigDecimal originPrice;
    private BigDecimal salePrice;
    private Integer totalStock;
    private Integer saleCount;
    private String unit;
    private Integer status;
    private Integer isSeckill;
    private Integer isGroup;
    private Integer delFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
