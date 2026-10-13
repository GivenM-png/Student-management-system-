package com.studenthub.backend.audit;

import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository {
    private static final RowMapper<AuditEntry> AUDIT_ENTRY_MAPPER = (rs, rowNum) -> new AuditEntry(
            rs.getLong("id"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getString("actor_email"),
            rs.getString("actor_role"),
            rs.getString("action"),
            rs.getString("entity_type"),
            rs.getString("entity_id"),
            rs.getString("details"));

    private final JdbcTemplate jdbc;

    public AuditLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void ensureTableExists() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actor_email VARCHAR(254) NULL,
                    actor_role VARCHAR(16) NULL,
                    action VARCHAR(80) NOT NULL,
                    entity_type VARCHAR(64) NOT NULL,
                    entity_id VARCHAR(128) NULL,
                    details VARCHAR(1000) NULL,
                    PRIMARY KEY (id)
                )
                """);
    }

    public void log(String actorEmail, String actorRole, String action, String entityType,
                    String entityId, String details) {
        jdbc.update("""
                INSERT INTO audit_logs (actor_email, actor_role, action, entity_type, entity_id, details)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                actorEmail == null ? null : actorEmail.trim(),
                actorRole == null ? null : actorRole.trim(),
                action == null ? "UNKNOWN" : action.trim(),
                entityType == null ? "system" : entityType.trim(),
                entityId == null ? null : entityId.trim(),
                details == null ? null : details.trim());
    }

    public List<AuditEntry> findRecent(int limit) {
        return jdbc.query("""
                SELECT id, created_at, actor_email, actor_role, action, entity_type, entity_id, details
                FROM audit_logs
                ORDER BY created_at DESC
                LIMIT ?
                """, AUDIT_ENTRY_MAPPER, limit);
    }

    public long countRows(String tableName) {
        String sanitized = tableName.trim();
        if (!sanitized.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid table name");
        }
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + sanitized, Long.class);
        return count == null ? 0L : count;
    }

    public record AuditEntry(long id, Instant createdAt, String actorEmail, String actorRole,
                             String action, String entityType, String entityId, String details) {}
}
