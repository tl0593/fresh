package com.fresh.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fresh.order.dto.GoodsSalesItemVO;
import com.fresh.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT oi.goods_id AS goodsId, MAX(oi.goods_name) AS goodsName, "
            + "SUM(oi.num) AS saleNum, SUM(oi.sub_total) AS saleAmount "
            + "FROM order_item oi "
            + "INNER JOIN order_main om ON oi.order_id = om.id "
            + "WHERE om.del_flag = 0 "
            + "AND om.status IN (1, 2, 4, 5) "
            + "AND om.pay_time >= #{start} "
            + "AND om.pay_time < #{end} "
            + "GROUP BY oi.goods_id")
    List<GoodsSalesItemVO> aggregatePaidSales(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);
}
