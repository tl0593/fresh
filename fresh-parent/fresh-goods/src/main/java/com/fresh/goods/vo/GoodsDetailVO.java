package com.fresh.goods.vo;

import com.fresh.goods.entity.GoodsImage;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GoodsDetailVO {

    private Long id;
    private String goodsName;
    private String goodsImg;
    private String goodsDesc;
    private BigDecimal salePrice;
    private Integer totalStock;
    private Integer saleCount;
    private String unit;
    private List<com.fresh.goods.entity.GoodsSpec> specs;
    private List<GoodsImage> images;
    private CommentRateVO commentRate;
}
