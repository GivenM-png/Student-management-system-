package com.studenthub.backend.api;

import com.studenthub.backend.audit.AuditLogService;
import com.studenthub.backend.course.CourseEnrollmentRepository;
import com.studenthub.backend.course.CourseEnrollmentRepository.CourseEnrollment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrollments")
public class CourseEnrollmentController {
    private final CourseEnrollmentRepository enrollments;
    private final AuditLogService auditLogs;

    public CourseEnrollmentController(CourseEnrollmentRepository enrollments, AuditLogService auditLogs) {
        this.enrollments = enrollments;
        this.auditLogs = auditLogs;
    }

    @GetMapping
    public List<CourseEnrollment> myEnrollments(
            @RequestAttribute("authenticatedEmail") String email) {
        return enrollments.findForAccount(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseEnrollment enroll(
            @RequestAttribute("authenticatedEmail") String email,
            @RequestAttribute("authenticatedRole") String role,
            @Valid @RequestBody EnrollRequest request) {
        CourseEnrollment enrollment = enrollments.enroll(email, request.moduleCode());
        auditLogs.record(email, role, "ENROLL_MODULE", "enrollment", enrollment.enrollmentId(),
                "Enrolled in module " + request.moduleCode());
        return enrollment;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(
            @RequestAttribute("authenticatedEmail") String email,
            @RequestAttribute("authenticatedRole") String role,
            @PathVariable long id) {
        enrollments.withdraw(email, id);
        auditLogs.record(email, role, "WITHDRAW_MODULE", "enrollment", Long.toString(id),
                "Withdrew from enrollment");
    }

    public record EnrollRequest(@NotBlank @Size(max = 32) String moduleCode) {}

}
