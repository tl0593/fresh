package com.fresh.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("group_activity")
public class GroupActivity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long goodsId;
    private Long specId;
    private BigDecimal groupPrice;
    private Integer groupNum;
    private Integer stock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String groupDesc;
    private Integer status;
    private Integer delFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 关联商品信息（非表字段） */
    @TableField(exist = false)
    private String goodsName;
    @TableField(exist = false)
    private String goodsImg;
    @TableField(exist = false)
    private BigDecimal originPrice;
    @TableField(exist = false)
    private BigDecimal salePrice;
}
