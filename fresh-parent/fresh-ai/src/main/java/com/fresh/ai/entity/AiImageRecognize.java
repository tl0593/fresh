package com.fresh.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_image_recognize")
public class AiImageRecognize {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long afterSaleId;
    private String imgUrl;
    private String rawResult;
    private Integer damageLevel;
    private BigDecimal damageRatio;
    private BigDecimal refundAmount;
    private LocalDateTime createTime;
}
