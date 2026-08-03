package com.fresh.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("goods_spec")
public class GoodsSpec {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long goodsId;
    private String specName;
    private BigDecimal specPrice;
    private Integer stock;
    private Integer isDefault;
    private Integer delFlag;
    private LocalDateTime createTime;
}
