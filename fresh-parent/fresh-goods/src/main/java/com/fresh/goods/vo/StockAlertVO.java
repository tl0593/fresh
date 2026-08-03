package com.fresh.goods.vo;

import lombok.Data;

@Data
public class StockAlertVO {

    private Long goodsId;
    private String goodsName;
    private String goodsImg;
    private Integer goodsStatus;
    private String unit;
    private Integer totalStock;
    private Long specId;
    private String specName;
    private Integer stock;
    /** OUT 缺货 / LOW 库存不足 */
    private String level;
    private Integer threshold;
}
