package com.studenthub.backend.student;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(String id) {
        super("No student found with ID " + id + ".");
    }
}
