package com.studenthub.backend.course;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CourseEnrollmentRepository {
    private final JdbcTemplate jdbc;
    private final CourseCatalogRepository catalog;

    public CourseEnrollmentRepository(JdbcTemplate jdbc, CourseCatalogRepository catalog) {
        this.jdbc = jdbc;
        this.catalog = catalog;
    }

    public List<CourseEnrollment> findForAccount(String email) {
        return find("WHERE e.account_email = ?", email);
    }

    public List<CourseEnrollment> findAll() {
        return find("", null);
    }

    private List<CourseEnrollment> find(String filter, Object parameter) {
        String sql = """
                SELECT e.id, e.account_email, c.faculty, c.course_code, c.course_name, m.module_code,
                       m.module_name, m.credits, e.price_at_enrolment, e.enrolled_at
                FROM enrollments e
                JOIN course_modules m ON m.module_code = e.module_code
                JOIN courses c ON c.course_code = m.course_code
                """ + filter + " ORDER BY c.course_name, m.module_code";
        var mapper = (org.springframework.jdbc.core.RowMapper<CourseEnrollment>) (rs, row) ->
                new CourseEnrollment(
                        Long.toString(rs.getLong("id")),
                        rs.getString("account_email"),
                        rs.getString("faculty"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("module_code"),
                        rs.getString("module_name"),
                        Integer.toString(rs.getInt("credits")),
                        rs.getBigDecimal("price_at_enrolment").toPlainString(),
                        rs.getTimestamp("enrolled_at").toInstant().toString());
        return parameter == null ? jdbc.query(sql, mapper) : jdbc.query(sql, mapper, parameter);
    }

    @Transactional
    public CourseEnrollment enroll(String email, String moduleCode) {
        String normalizedCode = moduleCode.trim().toUpperCase(java.util.Locale.ROOT);
        var price = catalog.findModulePrice(normalizedCode);
        if (price == null) {
            throw new CourseModuleNotFoundException(normalizedCode);
        }
        jdbc.update("INSERT INTO enrollments (account_email, module_code, price_at_enrolment) "
                        + "VALUES (?, ?, ?)",
                email, normalizedCode, price);
        return findForAccount(email).stream()
                .filter(enrollment -> enrollment.moduleCode().equals(normalizedCode))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The enrollment could not be loaded."));
    }

    public void withdraw(String email, long enrollmentId) {
        int changed = jdbc.update("DELETE FROM enrollments WHERE id = ? AND account_email = ?",
                enrollmentId, email);
        if (changed == 0) {
            throw new EnrollmentNotFoundException();
        }
    }

    public record CourseEnrollment(String enrollmentId, String accountEmail, String faculty, String courseCode,
                                   String courseName, String moduleCode, String moduleName,
                                   String credits, String priceZar, String enrolledAt) {}
}
