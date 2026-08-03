package com.fresh.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("goods_sales_stat")
public class GoodsSalesStat {

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Long goodsId;
    private Integer saleNum;
    private BigDecimal saleAmount;
    /** 非持久化：聚合时从订单明细带出 */
    @TableField(exist = false)
    private String goodsName;
}
