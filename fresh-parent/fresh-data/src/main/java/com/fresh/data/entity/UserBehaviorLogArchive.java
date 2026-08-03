package com.fresh.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_behavior_log_archive")
public class UserBehaviorLogArchive {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer behaviorType;
    private Long goodsId;
    private LocalDateTime createTime;
}
