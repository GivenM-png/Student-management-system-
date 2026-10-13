package com.studenthub.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studenthub.backend.audit.AuditLogRepository;
import com.studenthub.backend.audit.AuditLogService;
import com.studenthub.backend.auth.AuthService;
import com.studenthub.backend.auth.PasswordHasher;
import com.studenthub.backend.auth.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {
    @Test
    void rejectsMalformedAuthorizationHeaderOnLogout() throws Exception {
        AuthService authService = new AuthService(null, new PasswordHasher());
        TokenService tokenService = new TokenService();
        AuditLogService auditLogs = new AuditLogService(new AuditLogRepository(null));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, tokenService, auditLogs))
                .setControllerAdvice(new ApiErrorHandler())
                .build();

        mvc.perform(post("/api/auth/logout").header("Authorization", "bad-header"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("A valid bearer token is required."));
    }
}
