package com.studenthub.backend.audit;

import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogs;

    public AuditLogService(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    public void record(String actorEmail, String actorRole, String action, String entityType,
                       String entityId, String details) {
        auditLogs.log(actorEmail, actorRole, action, entityType, entityId, details);
    }
}
