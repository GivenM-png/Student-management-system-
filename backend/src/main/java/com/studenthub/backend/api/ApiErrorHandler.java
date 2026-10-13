package com.studenthub.backend.api;

import com.studenthub.backend.auth.AuthException;
import com.studenthub.backend.course.CourseModuleNotFoundException;
import com.studenthub.backend.course.EnrollmentNotFoundException;
import com.studenthub.backend.student.StudentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuth(AuthException e) {
        return ResponseEntity.status(e.status()).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateKeyException e,
                                                                HttpServletRequest request) {
        String message = request.getRequestURI().startsWith("/api/auth")
                ? "An account already exists for that email."
                : request.getRequestURI().startsWith("/api/enrollments")
                ? "You are already enrolled in that module."
                : "A student with that ID already exists.";
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", message));
    }

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(StudentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler({CourseModuleNotFoundException.class, EnrollmentNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleCourseNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        FieldError error = e.getBindingResult().getFieldErrors().get(0);
        return ResponseEntity.badRequest().body(Map.of(
                "message", error.getDefaultMessage() == null ? "Invalid request." : error.getDefaultMessage()));
    }
}
