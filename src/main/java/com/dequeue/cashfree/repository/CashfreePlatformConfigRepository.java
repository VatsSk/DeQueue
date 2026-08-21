package com.dequeue.cashfree.repository;

import com.dequeue.cashfree.entity.CashfreePlatformConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashfreePlatformConfigRepository extends MongoRepository<CashfreePlatformConfig, String> {
    // Singleton document with id = "global"
}
