package com.fresh.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_group_text")
public class AiGroupText {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private Long goodsId;
    private String inputInfo;
    private String outputText;
    private LocalDateTime createTime;
}
