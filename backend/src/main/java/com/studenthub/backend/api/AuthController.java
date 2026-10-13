package com.studenthub.backend.api;

import com.studenthub.backend.audit.AuditLogService;
import com.studenthub.backend.auth.AuthService;
import com.studenthub.backend.auth.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final TokenService tokenService;
    private final AuditLogService auditLogs;

    public AuthController(AuthService authService, TokenService tokenService, AuditLogService auditLogs) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.auditLogs = auditLogs;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        AuthService.Account account = authService.register(
                request.name().trim(), request.surname().trim(), request.email().trim(),
                request.gender(), request.birthDate(), request.phone(), request.address(),
                request.courseInterest(), request.password());
        auditLogs.record(account.email(), account.role(), "REGISTER", "account", account.email(),
                "Created a student application awaiting approval");
        return response("", account);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.Account account = authService.authenticate(
                request.email().trim(), request.password());
        String token = tokenService.issue(account.email(), account.role());
        auditLogs.record(account.email(), account.role(), "LOGIN", "account", account.email(),
                "Authenticated to StudentHub");
        return response(token, account);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractToken(authorization);
        String email = tokenService.emailForValidToken(token);
        String role = tokenService.roleForValidToken(token);
        tokenService.revoke(token);
        auditLogs.record(email, role, "LOGOUT", "session", token, "Revoked current session token");
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new com.studenthub.backend.auth.AuthException(HttpStatus.UNAUTHORIZED,
                    "A valid bearer token is required.");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw new com.studenthub.backend.auth.AuthException(HttpStatus.UNAUTHORIZED,
                    "A valid bearer token is required.");
        }
        return token;
    }

    @org.springframework.web.bind.annotation.GetMapping("/me")
    public AuthResponse currentAccount(
            @org.springframework.web.bind.annotation.RequestAttribute("authenticatedEmail") String email,
            @org.springframework.web.bind.annotation.RequestAttribute("authenticatedRole") String role) {
        AuthService.Account account = authService.findAccount(email);
        return response("", account);
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 100) String surname,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "Female|Male|Non-binary|Prefer not to say") String gender,
            @jakarta.validation.constraints.NotNull @Past LocalDate birthDate,
            @Size(max = 40) String phone,
            @Size(max = 500) String address,
            @Size(max = 150) String courseInterest,
            @NotBlank @Size(min = 12, max = 200) String password) {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 200) String password) {}

    private AuthResponse response(String token, AuthService.Account account) {
        return new AuthResponse(token, account.name(), account.surname(), account.email(), account.role(),
                account.approvalStatus(), account.gender(), account.birthDate(), account.phone(),
                account.address(), account.courseInterest(), account.studentId());
    }

    public record AuthResponse(String token, String name, String surname, String email, String role,
                               String approvalStatus, String gender, String birthDate,
                               String phone, String address, String courseInterest, String studentId) {}
}
