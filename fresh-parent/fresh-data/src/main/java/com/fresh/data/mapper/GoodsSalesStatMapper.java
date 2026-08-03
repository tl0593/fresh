package com.fresh.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fresh.data.entity.GoodsSalesStat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GoodsSalesStatMapper extends BaseMapper<GoodsSalesStat> {

    @Insert("""
            INSERT INTO goods_sales_stat (stat_date, goods_id, sale_num, sale_amount)
            VALUES (#{statDate}, #{goodsId}, #{saleNum}, #{saleAmount})
            ON DUPLICATE KEY UPDATE
              sale_num = VALUES(sale_num),
              sale_amount = VALUES(sale_amount)
            """)
    int upsert(GoodsSalesStat row);
}
