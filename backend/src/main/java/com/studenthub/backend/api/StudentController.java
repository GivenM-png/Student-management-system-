package com.studenthub.backend.api;

import com.studenthub.backend.audit.AuditLogService;
import com.studenthub.backend.student.StudentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentRepository students;
    private final AuditLogService auditLogs;

    public StudentController(StudentRepository students, AuditLogService auditLogs) {
        this.students = students;
        this.auditLogs = auditLogs;
    }

    @GetMapping
    public List<StudentResponse> list() {
        return students.findAll();
    }

    @PutMapping("/{id}")
    public StudentResponse update(@RequestAttribute("authenticatedEmail") String actorEmail,
                                 @RequestAttribute("authenticatedRole") String actorRole,
                                 @PathVariable String id, @Valid @RequestBody StudentRequest request) {
        StudentResponse updated = students.update(id, request);
        auditLogs.record(actorEmail, actorRole, "UPDATE_STUDENT", "student", id,
                "Updated student record for " + request.name());
        return updated;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute("authenticatedEmail") String actorEmail,
                       @RequestAttribute("authenticatedRole") String actorRole,
                       @PathVariable String id) {
        students.delete(id);
        auditLogs.record(actorEmail, actorRole, "DELETE_STUDENT", "student", id,
                "Deleted student record");
    }

    public record StudentRequest(
            @NotBlank @Size(max = 64) String id,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 254) String email,
            @Size(max = 150) String course,
            @Size(max = 40) String phone) {}

    public record StudentResponse(String id, String name, String email, String course, String phone) {}
}
