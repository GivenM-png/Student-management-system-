package com.studenthub.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TokenAuthInterceptor implements HandlerInterceptor {
    private final TokenService tokens;
    private final ObjectMapper objectMapper;

    public TokenAuthInterceptor(TokenService tokens, ObjectMapper objectMapper) {
        this.tokens = tokens;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String email = tokens.emailForValidToken(authorization.substring(7));
            if (email != null) {
                request.setAttribute("authenticatedEmail", email);
                request.setAttribute("authenticatedRole",
                        tokens.roleForValidToken(authorization.substring(7)));
                return true;
            }
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                Map.of("message", "Sign in to access student records."));
        return false;
    }
}
