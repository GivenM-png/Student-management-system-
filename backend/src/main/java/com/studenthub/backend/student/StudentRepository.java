package com.studenthub.backend.student;

import com.studenthub.backend.api.StudentController.StudentRequest;
import com.studenthub.backend.api.StudentController.StudentResponse;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StudentRepository {
    private final JdbcTemplate jdbc;

    public StudentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<StudentResponse> findAll() {
        return jdbc.query("SELECT student_id, full_name, email, course, phone "
                        + "FROM students ORDER BY student_id",
                (rs, row) -> new StudentResponse(rs.getString("student_id"),
                        rs.getString("full_name"), rs.getString("email"),
                        rs.getString("course"), rs.getString("phone")));
    }

    public StudentResponse create(StudentRequest student) {
        jdbc.update("INSERT INTO students (student_id, full_name, email, course, phone) "
                        + "VALUES (?, ?, ?, ?, ?)",
                student.id().trim(), student.name().trim(), valueOrEmpty(student.email()),
                valueOrEmpty(student.course()), valueOrEmpty(student.phone()));
        return findById(student.id().trim());
    }

    public StudentResponse update(String id, StudentRequest student) {
        String normalizedId = id.trim();
        int changed = jdbc.update("UPDATE students SET full_name = ?, email = ?, course = ?, phone = ? "
                        + "WHERE student_id = ?",
                student.name().trim(), valueOrEmpty(student.email()), valueOrEmpty(student.course()),
                valueOrEmpty(student.phone()), normalizedId);
        if (changed == 0) {
            throw new StudentNotFoundException(normalizedId);
        }
        return findById(normalizedId);
    }

    public void delete(String id) {
        int changed = jdbc.update("DELETE FROM students WHERE student_id = ?", id.trim());
        if (changed == 0) {
            throw new StudentNotFoundException(id);
        }
    }

    private StudentResponse findById(String id) {
        return jdbc.queryForObject("SELECT student_id, full_name, email, course, phone "
                        + "FROM students WHERE student_id = ?",
                (rs, row) -> new StudentResponse(rs.getString("student_id"),
                        rs.getString("full_name"), rs.getString("email"),
                        rs.getString("course"), rs.getString("phone")), id);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
