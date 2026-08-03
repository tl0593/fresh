package com.fresh.goods.dto;

import com.fresh.goods.entity.Goods;
import com.fresh.goods.entity.GoodsSpec;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GoodsSaveWithSpecsDTO {

    private Goods goods;
    /** 销售规格（SKU），至少 1 条 */
    private List<GoodsSpec> specs = new ArrayList<>();
}
