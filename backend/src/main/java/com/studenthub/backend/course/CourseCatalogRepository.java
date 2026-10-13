package com.studenthub.backend.course;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CourseCatalogRepository {
    private final JdbcTemplate jdbc;

    public CourseCatalogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CourseModule> findAllModules() {
        return jdbc.query("""
                SELECT c.faculty, c.course_code, c.course_name, m.module_code,
                       m.module_name, m.credits, m.price_zar
                FROM courses c
                JOIN course_modules m ON m.course_code = c.course_code
                ORDER BY c.faculty, c.course_name, m.module_code
                """, (rs, row) -> new CourseModule(
                rs.getString("faculty"),
                rs.getString("course_code"),
                rs.getString("course_name"),
                rs.getString("module_code"),
                rs.getString("module_name"),
                Integer.toString(rs.getInt("credits")),
                rs.getBigDecimal("price_zar").toPlainString()));
    }

    public BigDecimal findModulePrice(String moduleCode) {
        return jdbc.query("SELECT price_zar FROM course_modules WHERE module_code = ?",
                rs -> rs.next() ? rs.getBigDecimal(1) : null, moduleCode);
    }

    public void updateModulePrice(String moduleCode, BigDecimal price) {
        String normalizedCode = moduleCode.trim().toUpperCase(java.util.Locale.ROOT);
        if (findModulePrice(normalizedCode) == null) {
            throw new CourseModuleNotFoundException(moduleCode);
        }
        jdbc.update("UPDATE course_modules SET price_zar = ? WHERE module_code = ?",
                price, normalizedCode);
    }

    public record CourseModule(String faculty, String courseCode, String courseName,
                               String moduleCode, String moduleName, String credits, String priceZar) {}
}
