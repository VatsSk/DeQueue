package com.dequeue.common.audit;

import com.dequeue.common.security.SecurityUtils;
import com.dequeue.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    
    private final AuditRepository auditRepository;
    
    @Override
    @Async
    public void log(String vendorId, String staffId, String staffName, String action, String entityType, String entityId, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .vendorId(vendorId)
                    .staffId(staffId)
                    .staffName(staffName)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .timestamp(Instant.now())
                    .build();
            auditRepository.save(auditLog);
            log.debug("Audit log created: {} - {} - {}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }
    
    @Override
    @Async
    public void log(String action, String entityType, String entityId, String details) {
        try {
            UserPrincipal principal = SecurityUtils.getCurrentUserPrincipal();
            log(principal.getVendorId(), principal.getId(), principal.getName(), action, entityType, entityId, details);
        } catch (Exception e) {
            log(null, null, "SYSTEM", action, entityType, entityId, details);
        }
    }
}
