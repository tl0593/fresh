package com.fresh.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fresh.data")
public class DataProperties {

    /** 实时指标缓存 TTL（秒） */
    private long realStatTtl = 86400L;
    /** 每日统计 cron */
    private String dailyStatCron = "0 0 1 * * ?";
    /** 行为日志归档阈值（天） */
    private int archiveDay = 30;
    /** 查询区间最大天数 */
    private int maxQueryDays = 90;
}
