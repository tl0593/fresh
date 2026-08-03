package com.fresh.goods.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class GoodsRedisService {

    private final StringRedisTemplate redisTemplate;

    private static final String SECKILL_COUPON_LUA = """
            local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
            if stock <= 0 then return -1 end
            if redis.call('EXISTS', KEYS[2]) == 1 then return -2 end
            redis.call('DECR', KEYS[1])
            redis.call('SET', KEYS[2], '1', 'EX', ARGV[1])
            return 1
            """;

    public boolean tryLock(String key, long waitSeconds, long holdSeconds) {
        long deadline = System.currentTimeMillis() + waitSeconds * 1000;
        String value = UUID.randomUUID().toString();
        while (System.currentTimeMillis() < deadline) {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(holdSeconds));
            if (Boolean.TRUE.equals(ok)) {
                return true;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public void unlock(String key) {
        redisTemplate.delete(key);
    }

    public Long seckillCouponGrab(String stockKey, String userKey, long userTtlSeconds) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(SECKILL_COUPON_LUA);
        script.setResultType(Long.class);
        return redisTemplate.execute(script, List.of(stockKey, userKey), String.valueOf(userTtlSeconds));
    }

    public Long decrementStock(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    public void setStock(String key, int stock) {
        redisTemplate.opsForValue().set(key, String.valueOf(stock));
    }

    public Integer getStock(String key) {
        String val = redisTemplate.opsForValue().get(key);
        return val == null ? null : Integer.parseInt(val);
    }

    public void incrementUserLimit(String key) {
        redisTemplate.opsForValue().increment(key);
    }

    public int getUserLimit(String key) {
        String val = redisTemplate.opsForValue().get(key);
        return val == null ? 0 : Integer.parseInt(val);
    }

    public void deleteKeys(String... keys) {
        redisTemplate.delete(List.of(keys));
    }
}
