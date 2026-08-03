package com.fresh.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fresh.data.entity.DailyStat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DailyStatMapper extends BaseMapper<DailyStat> {

    @Insert("""
            INSERT INTO daily_stat
            (stat_date, new_user, active_user, order_count, order_amount, group_success_num, after_sale_num)
            VALUES
            (#{statDate}, #{newUser}, #{activeUser}, #{orderCount}, #{orderAmount}, #{groupSuccessNum}, #{afterSaleNum})
            ON DUPLICATE KEY UPDATE
              new_user = VALUES(new_user),
              active_user = VALUES(active_user),
              order_count = VALUES(order_count),
              order_amount = VALUES(order_amount),
              group_success_num = VALUES(group_success_num),
              after_sale_num = VALUES(after_sale_num)
            """)
    int upsert(DailyStat daily);
}
