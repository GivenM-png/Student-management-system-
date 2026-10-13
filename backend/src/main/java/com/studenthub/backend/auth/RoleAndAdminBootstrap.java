package com.studenthub.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleAndAdminBootstrap implements ApplicationRunner {
    private final AccountRepository accounts;
    private final AuthService authService;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;

    public RoleAndAdminBootstrap(
            AccountRepository accounts,
            AuthService authService,
            @Value("${app.admin.name:}") String adminName,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.authService = authService;
        this.adminName = adminName.trim();
        this.adminEmail = adminEmail.trim();
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        accounts.addRoleColumnIfMissing();
        accounts.addApprovalStatusColumnIfMissing();
        accounts.addStudentProfileColumnsIfMissing();
        boolean configured = !adminName.isEmpty() && !adminEmail.isEmpty() && !adminPassword.isEmpty();
        if (!configured) {
            if (!adminName.isEmpty() || !adminEmail.isEmpty() || !adminPassword.isEmpty()) {
                throw new IllegalStateException("Configure ADMIN_NAME, ADMIN_EMAIL, and ADMIN_PASSWORD together.");
            }
            return;
        }
        if (adminPassword.length() < 16) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least 16 characters.");
        }
        authService.ensureAdmin(adminName, adminEmail, adminPassword);
    }
}
