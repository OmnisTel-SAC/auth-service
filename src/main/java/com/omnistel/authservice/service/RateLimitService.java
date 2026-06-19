package com.omnistel.authservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_SECONDS = 900;

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isBlocked(String username) {
        String count = redisTemplate.opsForValue().get(key(username));
        return count != null && Integer.parseInt(count) >= MAX_ATTEMPTS;
    }

    public void recordFailedAttempt(String username) {
        String redisKey = key(username);
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, BLOCK_DURATION_SECONDS, TimeUnit.SECONDS);
        }
    }

    public void resetAttempts(String username) {
        redisTemplate.delete(key(username));
    }

    private String key(String username) {
        return "ratelimit:login:user:" + username.toLowerCase();
    }
}
