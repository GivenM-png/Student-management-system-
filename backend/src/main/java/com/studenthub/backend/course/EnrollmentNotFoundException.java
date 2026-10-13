package com.studenthub.backend.course;

public class EnrollmentNotFoundException extends RuntimeException {
    public EnrollmentNotFoundException() {
        super("That enrollment could not be found.");
    }
}
