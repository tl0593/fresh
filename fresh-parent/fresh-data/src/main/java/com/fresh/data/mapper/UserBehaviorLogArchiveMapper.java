package com.fresh.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fresh.data.entity.UserBehaviorLogArchive;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface UserBehaviorLogArchiveMapper extends BaseMapper<UserBehaviorLogArchive> {

    @Insert("""
            INSERT INTO user_behavior_log_archive (id, user_id, behavior_type, goods_id, create_time)
            SELECT id, user_id, behavior_type, goods_id, create_time
            FROM user_behavior_log
            WHERE create_time < #{beforeTime}
            """)
    int copyFromMain(@Param("beforeTime") LocalDateTime beforeTime);
}
