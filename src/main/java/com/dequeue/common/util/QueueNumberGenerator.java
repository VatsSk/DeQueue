package com.dequeue.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueNumberGenerator {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final MongoTemplate mongoTemplate;
    
    public String generateQueueNumber(String vendorId, String prefix) {
        String key = "queue:" + vendorId + ":counter";
        Long counter = null;
        
        try {
            counter = redisTemplate.opsForValue().increment(key);
            
            // Self-healing: if Redis thinks this is the very first order, check if we actually lost data
            if (counter != null && counter == 1L) {
                Query query = new Query();
                query.addCriteria(Criteria.where("vendorId").is(vendorId));
                long actualCount = mongoTemplate.count(query, "orders");
                
                if (actualCount > 0) {
                    // Redis lost data! Sync the actual count back to Redis
                    counter = actualCount + 1;
                    redisTemplate.opsForValue().set(key, counter);
                    System.out.println("Redis self-healed counter from MongoDB. New counter: " + counter);
                }
            }
        } catch (Exception e) {
            // Fallback to MongoDB if Redis is down
            System.err.println("Redis unavailable, using MongoDB fallback for queue number. Error: " + e.getMessage());
            Query query = new Query();
            query.addCriteria(Criteria.where("vendorId").is(vendorId));
            long count = mongoTemplate.count(query, "orders");
            counter = count + 1;
        }
        
        long val = counter != null ? counter : 1L;
        
        // Cycle from A0001 to Z9999
        long index = (val - 1) % (26 * 9999);
        long letterIndex = index / 9999;
        long number = (index % 9999) + 1;
        
        char letter = (char) ('A' + letterIndex);
        
        return String.format("%c%04d", letter, number);
    }
    
    public void resetDailyCounter(String vendorId) {
        // Counter is no longer daily, nothing to reset
    }
}
