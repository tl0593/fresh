package com.fresh.data.job;

import com.fresh.data.service.BehaviorArchiveService;
import com.fresh.data.service.DailyStatAggregateService;
import com.fresh.data.service.UserBehaviorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataScheduleTask {

    private final DailyStatAggregateService dailyStatAggregateService;
    private final BehaviorArchiveService behaviorArchiveService;
    private final UserBehaviorService userBehaviorService;

    @Scheduled(cron = "${fresh.data.daily-stat-cron:0 0 1 * * ?}")
    public void runDailyStat() {
        try {
            userBehaviorService.flushBuffer();
            LocalDate yesterday = LocalDate.now().minusDays(1);
            dailyStatAggregateService.aggregateForDate(yesterday);
        } catch (Exception e) {
            log.error("daily stat job failed", e);
        }
    }

    @Scheduled(cron = "0 30 1 * * ?")
    public void runArchive() {
        try {
            userBehaviorService.flushBuffer();
            behaviorArchiveService.archiveExpiredLogs();
        } catch (Exception e) {
            log.error("behavior archive job failed", e);
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void flushBehaviorBuffer() {
        userBehaviorService.flushBuffer();
    }
}
