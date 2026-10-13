package com.studenthub.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class StudentOnlyInterceptor implements HandlerInterceptor {
    private final ObjectMapper objectMapper;

    public StudentOnlyInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!"POST".equals(request.getMethod()) && !"DELETE".equals(request.getMethod())) {
            return true;
        }
        if ("STUDENT".equals(request.getAttribute("authenticatedRole"))) {
            return true;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                Map.of("message", "This enrolment action is reserved for student accounts."));
        return false;
    }
}
