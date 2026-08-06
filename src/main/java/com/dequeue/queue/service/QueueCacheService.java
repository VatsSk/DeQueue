package com.dequeue.queue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueCacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    
    public String getCurrentServing(String vendorId) {
        return (String) redisTemplate.opsForValue().get("queue:" + vendorId + ":serving");
    }
    
    public int getQueueLength(String vendorId) {
        Long size = redisTemplate.opsForZSet().size("queue:" + vendorId + ":active");
        return size != null ? size.intValue() : 0;
    }
    
    public void resetDailyQueue(String vendorId) {
        redisTemplate.delete("queue:" + vendorId + ":active");
        redisTemplate.delete("queue:" + vendorId + ":serving");
        redisTemplate.delete("queue:" + vendorId + ":status");
    }
}
