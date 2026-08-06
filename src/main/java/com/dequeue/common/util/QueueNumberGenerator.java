package com.dequeue.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class QueueNumberGenerator {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // In-memory fallback if Redis is not available
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong> fallbackCounters = new java.util.concurrent.ConcurrentHashMap<>();
    
    public String generateQueueNumber(String vendorId, String prefix) {
        String key = "queue:" + vendorId + ":counter:" + LocalDate.now();
        Long counter = null;
        
        try {
            counter = redisTemplate.opsForValue().increment(key);
            if (counter != null && counter == 1L) {
                redisTemplate.expire(key, Duration.ofHours(26));
            }
        } catch (Exception e) {
            // Fallback to in-memory counter if Redis is down
            System.err.println("Redis unavailable, using in-memory fallback for queue number. Error: " + e.getMessage());
            counter = fallbackCounters.computeIfAbsent(key, k -> new java.util.concurrent.atomic.AtomicLong(0)).incrementAndGet();
        }
        
        return String.format("%s%03d", prefix != null ? prefix : "Q", counter != null ? counter : 1);
    }
    
    public void resetDailyCounter(String vendorId) {
        String key = "queue:" + vendorId + ":counter:" + LocalDate.now();
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            // Ignore redis errors
        }
        fallbackCounters.remove(key);
    }
}
