package com.fresh.gateway.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fresh.common.constant.RedisKeyConstant;
import com.fresh.gateway.config.FreshGatewayProperties;
import com.fresh.gateway.entity.BlackIp;
import com.fresh.gateway.entity.LimitConfig;
import com.fresh.gateway.mapper.BlackIpMapper;
import com.fresh.gateway.mapper.LimitConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GatewayCacheService {

    private static final int DEFAULT_IP_LIMIT = 100;

    private final BlackIpMapper blackIpMapper;
    private final LimitConfigMapper limitConfigMapper;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final FreshGatewayProperties properties;

    private volatile List<LimitConfig> limitConfigs = List.of();

    @Scheduled(fixedDelay = 30000, initialDelay = 0)
    public void refreshCache() {
        List<BlackIp> blackList = blackIpMapper.selectList(new LambdaQueryWrapper<BlackIp>()
                .and(wrapper -> wrapper.isNull(BlackIp::getExpireTime)
                        .or()
                        .gt(BlackIp::getExpireTime, LocalDateTime.now())));
        for (BlackIp blackIp : blackList) {
            String key = RedisKeyConstant.BLACK_IP + blackIp.getIp();
            if (blackIp.getExpireTime() == null) {
                redisTemplate.opsForValue().set(key, "1").subscribe();
            } else {
                Duration ttl = Duration.between(LocalDateTime.now(), blackIp.getExpireTime());
                if (!ttl.isNegative() && !ttl.isZero()) {
                    redisTemplate.opsForValue().set(key, "1", ttl).subscribe();
                }
            }
        }

        limitConfigs = limitConfigMapper.selectList(new LambdaQueryWrapper<LimitConfig>()
                .eq(LimitConfig::getStatus, 1));
    }

    public Mono<Boolean> isBlackIp(String ip) {
        return redisTemplate.hasKey(RedisKeyConstant.BLACK_IP + ip)
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> isRateLimitExceeded(String path, String ip) {
        LimitConfig matched = matchLimitConfig(path);
        int limit = matched == null ? DEFAULT_IP_LIMIT : matched.getLimitCount();
        int timeSecond = matched == null ? properties.getLimitTimeSecond() : matched.getTimeSecond();
        String suffix = matched != null && matched.getLimitType() != null && matched.getLimitType() == 1
                ? "global" : ip;
        String key = RedisKeyConstant.LIMIT_API + path + ":" + suffix;

        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    Mono<Long> chain = Mono.just(count);
                    if (count != null && count == 1) {
                        chain = redisTemplate.expire(key, Duration.ofSeconds(timeSecond)).thenReturn(count);
                    }
                    return chain.map(c -> c != null && c > limit);
                })
                .defaultIfEmpty(false);
    }

    private LimitConfig matchLimitConfig(String path) {
        LimitConfig matched = null;
        for (LimitConfig config : limitConfigs) {
            if (path.startsWith(config.getApiPath())
                    && (matched == null || config.getApiPath().length() > matched.getApiPath().length())) {
                matched = config;
            }
        }
        return matched;
    }
}
