package com.fresh.gateway.service;

import com.fresh.gateway.config.FreshGatewayProperties;
import com.fresh.gateway.entity.GatewayAccessLog;
import com.fresh.gateway.mapper.GatewayAccessLogMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayAccessLogService {

    private static final int BATCH_SIZE = 50;

    private final GatewayAccessLogMapper accessLogMapper;
    private final FreshGatewayProperties properties;
    private final LinkedBlockingQueue<GatewayAccessLog> queue = new LinkedBlockingQueue<>();

    public void offer(GatewayAccessLog accessLog) {
        if (!properties.isAccessLogEnable()) {
            return;
        }
        queue.offer(accessLog);
        if (queue.size() >= BATCH_SIZE) {
            flush();
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void scheduledFlush() {
        flush();
    }

    @PreDestroy
    public void destroy() {
        flush();
    }

    private synchronized void flush() {
        List<GatewayAccessLog> batch = new ArrayList<>(BATCH_SIZE);
        queue.drainTo(batch, BATCH_SIZE);
        if (batch.isEmpty()) {
            return;
        }
        try {
            for (GatewayAccessLog accessLog : batch) {
                accessLogMapper.insert(accessLog);
            }
        } catch (Exception e) {
            log.error("批量写入网关访问日志失败, size={}", batch.size(), e);
            queue.addAll(batch);
        }
    }
}
