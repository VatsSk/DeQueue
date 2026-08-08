package com.dequeue.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class QueueNumberGenerator {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final MongoTemplate mongoTemplate;
    
    public String generateQueueNumber(String vendorId, String prefix) {
        String key = "queue:" + vendorId + ":counter:" + LocalDate.now();
        Long counter = null;
        
        try {
            counter = redisTemplate.opsForValue().increment(key);
            if (counter != null && counter == 1L) {
                redisTemplate.expire(key, Duration.ofHours(26));
            }
        } catch (Exception e) {
            // Fallback to MongoDB if Redis is down
            System.err.println("Redis unavailable, using MongoDB fallback for queue number. Error: " + e.getMessage());
            Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
            Query query = new Query();
            query.addCriteria(Criteria.where("vendorId").is(vendorId).and("createdAt").gte(startOfDay));
            long count = mongoTemplate.count(query, "orders");
            counter = count + 1;
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
    }
}
