package com.studenthub.backend.auth;

import java.util.Optional;
import java.sql.Date;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {
    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(String name, String surname, String email, String gender,
                       java.time.LocalDate birthDate, String phone, String address, String courseInterest,
                       PasswordHasher.HashedPassword password) {
        jdbc.update("""
                        INSERT INTO accounts
                            (full_name, surname, email, gender, date_of_birth, phone, home_address,
                             course_interest, role, approval_status, password_salt, password_hash)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'STUDENT', 'PENDING', ?, ?)
                        """,
                name, surname, email, gender, Date.valueOf(birthDate), phone, address, courseInterest,
                password.salt(), password.hash());
    }

    public void provisionAdmin(String name, String email, PasswordHasher.HashedPassword password) {
        jdbc.update("INSERT INTO accounts (full_name, email, role, approval_status, password_salt, password_hash) "
                        + "VALUES (?, ?, 'ADMIN', 'ACCEPTED', ?, ?) AS incoming ON DUPLICATE KEY UPDATE "
                        + "full_name = incoming.full_name, role = 'ADMIN', approval_status = 'ACCEPTED', "
                        + "password_salt = incoming.password_salt, password_hash = incoming.password_hash",
                name, email, password.salt(), password.hash());
    }

    public boolean hasColumn(String table, String column) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }

    public void addRoleColumnIfMissing() {
        if (!hasColumn("accounts", "role")) {
            jdbc.execute("ALTER TABLE accounts ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'STUDENT'");
        }
    }

    public void addApprovalStatusColumnIfMissing() {
        if (!hasColumn("accounts", "approval_status")) {
            jdbc.execute("ALTER TABLE accounts ADD COLUMN approval_status VARCHAR(16) "
                    + "NOT NULL DEFAULT 'ACCEPTED'");
        }
    }

    public void addStudentProfileColumnsIfMissing() {
        addColumnIfMissing("surname", "VARCHAR(100) NOT NULL DEFAULT ''");
        addColumnIfMissing("gender", "VARCHAR(32) NOT NULL DEFAULT ''");
        addColumnIfMissing("date_of_birth", "DATE NULL");
        addColumnIfMissing("phone", "VARCHAR(40) NOT NULL DEFAULT ''");
        addColumnIfMissing("home_address", "VARCHAR(500) NOT NULL DEFAULT ''");
        addColumnIfMissing("course_interest", "VARCHAR(150) NOT NULL DEFAULT ''");
        addColumnIfMissing("student_id", "VARCHAR(32) NULL");
        if (!hasIndex("uq_accounts_student_id")) {
            jdbc.execute("ALTER TABLE accounts ADD UNIQUE INDEX uq_accounts_student_id (student_id)");
        }
        jdbc.update("""
                UPDATE accounts SET student_id = CONCAT('STU-', YEAR(created_at), '-', LPAD(id, 6, '0'))
                WHERE role = 'STUDENT' AND approval_status = 'ACCEPTED' AND student_id IS NULL
                """);
        jdbc.update("""
                UPDATE accounts
                SET surname = TRIM(SUBSTRING(full_name, LOCATE(' ', full_name))),
                    full_name = SUBSTRING_INDEX(full_name, ' ', 1)
                WHERE role = 'STUDENT' AND surname = '' AND full_name LIKE '% %'
                """);
    }

    private void addColumnIfMissing(String column, String definition) {
        if (!hasColumn("accounts", column)) {
            jdbc.execute("ALTER TABLE accounts ADD COLUMN " + column + " " + definition);
        }
    }

    private boolean hasIndex(String index) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'accounts' AND index_name = ?
                """, Integer.class, index);
        return count != null && count > 0;
    }

    public Optional<AccountRecord> findByEmail(String email) {
        return jdbc.query("""
                        SELECT id, full_name, surname, email, role, approval_status, gender,
                               DATE_FORMAT(date_of_birth, '%Y-%m-%d') AS date_of_birth,
                               phone, home_address, course_interest, student_id, password_salt, password_hash
                        FROM accounts WHERE email = ?
                        """,
                (rs, row) -> mapAccount(rs), email).stream().findFirst();
    }

    public java.util.List<StudentApplication> findStudentApplications() {
        return jdbc.query("""
                        SELECT email, full_name, surname, gender,
                               DATE_FORMAT(date_of_birth, '%Y-%m-%d') AS date_of_birth,
                               phone, home_address, course_interest, approval_status, student_id
                        FROM accounts
                        WHERE role = 'STUDENT'
                        ORDER BY CASE approval_status WHEN 'PENDING' THEN 0 ELSE 1 END, created_at DESC
                        """,
                (rs, row) -> new StudentApplication(rs.getString("email"), rs.getString("full_name"),
                        rs.getString("surname"), rs.getString("gender"), rs.getString("date_of_birth"),
                        rs.getString("phone"), rs.getString("home_address"), rs.getString("course_interest"),
                        rs.getString("approval_status"), rs.getString("student_id")));
    }

    public int updatePendingStudentApproval(String email, String approvalStatus) {
        return jdbc.update("""
                        UPDATE accounts
                        SET approval_status = ?,
                            student_id = CASE WHEN ? = 'ACCEPTED'
                                THEN CONCAT('STU-', YEAR(CURRENT_DATE), '-', LPAD(id, 6, '0'))
                                ELSE NULL END
                        WHERE email = ? AND role = 'STUDENT' AND approval_status = 'PENDING'
                        """,
                approvalStatus, approvalStatus, email);
    }

    private AccountRecord mapAccount(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AccountRecord(rs.getLong("id"), rs.getString("full_name"), rs.getString("surname"),
                rs.getString("email"), rs.getString("role"), rs.getString("approval_status"),
                rs.getString("gender"), rs.getString("date_of_birth"), rs.getString("phone"),
                rs.getString("home_address"), rs.getString("course_interest"), rs.getString("student_id"),
                rs.getString("password_salt"), rs.getString("password_hash"));
    }

    public record AccountRecord(long id, String name, String surname, String email, String role,
                                String approvalStatus, String gender, String birthDate, String phone,
                                String address, String courseInterest, String studentId, String salt, String hash) {}

    public record StudentApplication(String email, String name, String surname, String gender, String birthDate,
                                     String phone, String address, String courseInterest,
                                     String approvalStatus, String studentId) {}
}
