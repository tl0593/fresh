package com.fresh.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_stat")
public class DailyStat {

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Integer newUser;
    private Integer activeUser;
    private Integer orderCount;
    private BigDecimal orderAmount;
    private Integer groupSuccessNum;
    private Integer afterSaleNum;
    private LocalDateTime createTime;
}
