package com.fresh.goods.vo;

import com.fresh.goods.entity.Goods;
import com.fresh.goods.entity.GoodsSpec;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GoodsAdminDetailVO {

    private Goods goods;
    private List<GoodsSpec> specs = new ArrayList<>();
}
