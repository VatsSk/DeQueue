package com.dequeue.common.audit;

public interface AuditService {
    void log(String vendorId, String staffId, String staffName, String action, String entityType, String entityId, String details);
    void log(String action, String entityType, String entityId, String details);
    
    default void logAction(String action, String details) {
        log(action, "UNKNOWN", null, details);
    }
}
