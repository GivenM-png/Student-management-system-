package com.studenthub.backend.course;

public class CourseModuleNotFoundException extends RuntimeException {
    public CourseModuleNotFoundException(String code) {
        super("No course module found with code " + code + ".");
    }
}
