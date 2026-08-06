package com.dequeue.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface AuditRepository extends MongoRepository<AuditLog, String> {
    Page<AuditLog> findByVendorId(String vendorId, Pageable pageable);
    List<AuditLog> findByVendorIdAndTimestampBetween(String vendorId, Instant start, Instant end);
    List<AuditLog> findByVendorIdAndEntityType(String vendorId, String entityType);
}
