package com.fresh.goods.dto;

import lombok.Data;

@Data
public class StockRestockDTO {

    /** 优先按规格补货 */
    private Long specId;
    /** 未传规格时按商品默认规格补货 */
    private Long goodsId;
    /** 本次增加数量，必须 > 0 */
    private Integer addNum;
}
