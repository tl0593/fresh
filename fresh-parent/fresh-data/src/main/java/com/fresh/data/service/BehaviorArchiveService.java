package com.fresh.data.service;

import com.fresh.data.config.DataProperties;
import com.fresh.data.mapper.UserBehaviorLogArchiveMapper;
import com.fresh.data.mapper.UserBehaviorLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorArchiveService {

    private final DataProperties dataProperties;
    private final UserBehaviorLogArchiveMapper archiveMapper;
    private final UserBehaviorLogMapper userBehaviorLogMapper;

    @Transactional(rollbackFor = Exception.class)
    public void archiveExpiredLogs() {
        LocalDateTime before = LocalDate.now().minusDays(dataProperties.getArchiveDay()).atStartOfDay();
        int copied = archiveMapper.copyFromMain(before);
        if (copied <= 0) {
            return;
        }
        int removed = userBehaviorLogMapper.deleteBefore(before);
        log.info("archived behavior logs before {}, copied={}, removed={}", before, copied, removed);
    }
}
