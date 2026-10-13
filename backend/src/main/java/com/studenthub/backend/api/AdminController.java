package com.studenthub.backend.api;

import com.studenthub.backend.audit.AuditLogRepository;
import com.studenthub.backend.audit.AuditLogService;
import com.studenthub.backend.auth.AuthService;
import com.studenthub.backend.course.CourseCatalogRepository;
import com.studenthub.backend.course.CourseEnrollmentRepository;
import com.studenthub.backend.course.CourseEnrollmentRepository.CourseEnrollment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final CourseEnrollmentRepository enrollments;
    private final CourseCatalogRepository catalog;
    private final AuthService authService;
    private final JdbcTemplate jdbc;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogs;

    public AdminController(CourseEnrollmentRepository enrollments, CourseCatalogRepository catalog,
                          AuthService authService, JdbcTemplate jdbc,
                          AuditLogRepository auditLogRepository, AuditLogService auditLogs) {
        this.enrollments = enrollments;
        this.catalog = catalog;
        this.authService = authService;
        this.jdbc = jdbc;
        this.auditLogRepository = auditLogRepository;
        this.auditLogs = auditLogs;
    }

    @GetMapping("/overview")
    public AdminOverview overview() {
        return new AdminOverview(
                jdbc.queryForObject("SELECT COUNT(*) FROM accounts "
                        + "WHERE role = 'STUDENT' AND approval_status = 'ACCEPTED'", Long.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM accounts", Long.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM enrollments", Long.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM course_modules", Long.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM accounts "
                        + "WHERE role = 'STUDENT' AND approval_status = 'ACCEPTED'", Long.class),
                auditLogRepository.findRecent(10));
    }

    @GetMapping("/enrollments")
    public List<CourseEnrollment> allEnrollments() {
        return enrollments.findAll();
    }

    @GetMapping("/applications")
    public List<com.studenthub.backend.auth.AccountRepository.StudentApplication> applications() {
        return authService.findStudentApplications();
    }

    @PutMapping("/applications/{email}/approval")
    public void updateApplicationApproval(
            @RequestAttribute("authenticatedEmail") String actorEmail,
            @RequestAttribute("authenticatedRole") String actorRole,
            @PathVariable String email,
            @Valid @RequestBody ApprovalRequest request) {
        String status = request.status().trim().toUpperCase(java.util.Locale.ROOT);
        authService.updateStudentApproval(email, status);
        auditLogs.record(actorEmail, actorRole, status.equals("ACCEPTED")
                        ? "ACCEPT_STUDENT_APPLICATION" : "DECLINE_STUDENT_APPLICATION",
                "account", email, "Set student account approval status to " + status);
    }

    @GetMapping(value = "/export/students", produces = "text/csv")
    public ResponseEntity<String> exportStudents() {
        String csv = jdbc.query("""
                        SELECT student_id, full_name, surname, email, gender, date_of_birth,
                               phone, home_address, course_interest, approval_status
                        FROM accounts WHERE role = 'STUDENT' ORDER BY created_at DESC
                        """,
                        (rs, rowNum) -> String.join(",",
                                quote(rs.getString("student_id")),
                                quote(rs.getString("full_name")),
                                quote(rs.getString("surname")),
                                quote(rs.getString("email")),
                                quote(rs.getString("gender")),
                                quote(rs.getString("date_of_birth")),
                                quote(rs.getString("phone")),
                                quote(rs.getString("home_address")),
                                quote(rs.getString("course_interest")),
                                quote(rs.getString("approval_status"))))
                .stream()
                .reduce("student_id,first_name,surname,email,gender,date_of_birth,phone,address,programme,status\n",
                        (acc, row) -> acc + row + "\n");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students.csv")
                .body(csv);
    }

    @GetMapping(value = "/export/enrollments", produces = "text/csv")
    public ResponseEntity<String> exportEnrollments() {
        String csv = jdbc.query("""
                        SELECT e.id, e.account_email, c.course_code, c.course_name, m.module_code, m.module_name,
                               m.credits, e.price_at_enrolment, e.enrolled_at
                        FROM enrollments e
                        JOIN course_modules m ON m.module_code = e.module_code
                        JOIN courses c ON c.course_code = m.course_code
                        ORDER BY e.id
                        """,
                        (rs, rowNum) -> String.join(",",
                                quote(rs.getString("id")),
                                quote(rs.getString("account_email")),
                                quote(rs.getString("course_code")),
                                quote(rs.getString("course_name")),
                                quote(rs.getString("module_code")),
                                quote(rs.getString("module_name")),
                                quote(rs.getString("credits")),
                                quote(rs.getString("price_at_enrolment")),
                                quote(rs.getTimestamp("enrolled_at").toString())) )
                .stream()
                .reduce("id,account_email,course_code,course_name,module_code,module_name,credits,price_at_enrolment,enrolled_at\n",
                        (acc, row) -> acc + row + "\n");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enrollments.csv")
                .body(csv);
    }

    @PutMapping("/modules/{moduleCode}/price")
    public void updateModulePrice(@RequestAttribute("authenticatedEmail") String actorEmail,
                                  @RequestAttribute("authenticatedRole") String actorRole,
                                  @PathVariable String moduleCode,
                                  @Valid @RequestBody PriceRequest request) {
        catalog.updateModulePrice(moduleCode, request.priceZar());
        auditLogs.record(actorEmail, actorRole, "UPDATE_MODULE_PRICE", "module", moduleCode,
                "Set module price to " + request.priceZar());
    }

    public record AdminOverview(long studentCount, long accountCount, long enrollmentCount,
                                long moduleCount, long profileCount,
                                List<AuditLogRepository.AuditEntry> recentActivity) {}

    public record ApprovalRequest(@NotBlank String status) {}

    private String quote(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return '"' + safe + '"';
        }
        return safe;
    }

    public record PriceRequest(
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal priceZar) {}
}
