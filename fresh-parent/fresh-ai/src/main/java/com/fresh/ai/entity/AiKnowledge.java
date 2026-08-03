package com.fresh.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_knowledge")
public class AiKnowledge {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String question;
    private String answer;
    private Integer sort;
    private Integer status;
    @TableLogic
    private Integer delFlag;
    private LocalDateTime createTime;
}
