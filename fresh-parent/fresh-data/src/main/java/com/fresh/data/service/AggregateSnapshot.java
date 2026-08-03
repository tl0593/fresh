package com.fresh.data.service;

import com.fresh.data.dto.GoodsSalesItemDTO;
import com.fresh.data.entity.DailyStat;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AggregateSnapshot {

    private DailyStat daily;
    private List<GoodsSalesItemDTO> goodsItems = new ArrayList<>();
}
