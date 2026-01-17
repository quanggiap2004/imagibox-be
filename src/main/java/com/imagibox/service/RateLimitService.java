package com.imagibox.service;

import com.imagibox.exception.QuotaExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${rate-limit.default-quota:10}")
    private int defaultQuota;

    @Value("${rate-limit.quota-expire-seconds:86400}")
    private long quotaExpireSeconds;

    private static final String QUOTA_KEY_PREFIX = "quota:user:";

    public void checkAndIncrementQuota(Long userId, int userQuota) {
        String key = QUOTA_KEY_PREFIX + userId + ":" + LocalDate.now();

        Integer currentUsage = (Integer) redisTemplate.opsForValue().get(key);
        if (currentUsage == null) {
            currentUsage = 0;
        }

        if (currentUsage >= userQuota) {
            log.warn("User {} exceeded daily quota: {}/{}", userId, currentUsage, userQuota);
            throw new QuotaExceededException("Đã vượt quá số lượng truyện có thể tạo trong ngày!");
        }

        // Increment usage
        redisTemplate.opsForValue().increment(key);

        // Set expiration to midnight next day
        if (currentUsage == 0) {
            redisTemplate.expire(key, Duration.ofSeconds(quotaExpireSeconds));
        }

        log.info("User {} quota usage: {}/{}", userId, currentUsage + 1, userQuota);
    }

    public int getRemainingQuota(Long userId, int userQuota) {
        String key = QUOTA_KEY_PREFIX + userId + ":" + LocalDate.now();
        Integer currentUsage = (Integer) redisTemplate.opsForValue().get(key);

        if (currentUsage == null) {
            return userQuota;
        }

        return Math.max(0, userQuota - currentUsage);
    }

    public void resetQuota(Long userId) {
        String key = QUOTA_KEY_PREFIX + userId + ":" + LocalDate.now();
        redisTemplate.delete(key);
        log.info("Reset quota for user {}", userId);
    }
}
