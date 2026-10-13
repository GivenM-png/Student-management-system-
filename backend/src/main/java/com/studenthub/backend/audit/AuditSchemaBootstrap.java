package com.studenthub.backend.audit;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AuditSchemaBootstrap implements ApplicationRunner {
    private final AuditLogRepository auditLogs;

    public AuditSchemaBootstrap(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    @Override
    public void run(ApplicationArguments args) {
        auditLogs.ensureTableExists();
    }
}
