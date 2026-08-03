package com.fresh.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fresh.data.dto.GoodsSalesItemDTO;
import com.fresh.data.entity.UserBehaviorLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface UserBehaviorLogMapper extends BaseMapper<UserBehaviorLog> {

    @Select("""
            SELECT COUNT(DISTINCT user_id) FROM user_behavior_log
            WHERE create_time >= #{start} AND create_time < #{end}
            """)
    Integer countActiveUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT COUNT(*) FROM user_behavior_log
            WHERE behavior_type = #{behaviorType}
              AND create_time >= #{start} AND create_time < #{end}
            """)
    Integer countByBehaviorType(@Param("behaviorType") int behaviorType,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    @Select("""
            SELECT COUNT(*) FROM (
                SELECT user_id FROM user_behavior_log
                GROUP BY user_id
                HAVING MIN(DATE(create_time)) = #{statDate}
            ) t
            """)
    Integer countNewUsers(@Param("statDate") LocalDate statDate);

    @Select("""
            SELECT goods_id AS goodsId, COUNT(*) AS saleNum, 0 AS saleAmount
            FROM user_behavior_log
            WHERE behavior_type = 3 AND goods_id IS NOT NULL
              AND create_time >= #{start} AND create_time < #{end}
            GROUP BY goods_id
            """)
    List<GoodsSalesItemDTO> aggregateGoodsSales(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Delete("DELETE FROM user_behavior_log WHERE create_time < #{beforeTime}")
    int deleteBefore(@Param("beforeTime") LocalDateTime beforeTime);
}
