package com.studenthub.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AuthServiceTest {
    @Test
    void rejectsWeakPasswordsDuringRegistration() {
        AccountRepository accounts = mock(AccountRepository.class);
        AuthService service = new AuthService(accounts, new PasswordHasher());

        AuthException exception = assertThrows(AuthException.class,
                () -> service.register("Alice", "Example", "alice@example.com", "Female",
                        java.time.LocalDate.of(2000, 1, 1), "", "", "",
                        "weak"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    }

    @Test
    void rejectsDuplicateEmails() {
        AccountRepository accounts = mock(AccountRepository.class);
        when(accounts.findByEmail("alice@example.com")).thenReturn(Optional.of(
                new AccountRepository.AccountRecord(1L, "Alice", "Example", "alice@example.com",
                        "STUDENT", "PENDING", "Female", "2000-01-01", "", "", "",
                        null, "salt", "hash")));
        AuthService service = new AuthService(accounts, new PasswordHasher());

        AuthException exception = assertThrows(AuthException.class,
                () -> service.register("Alice", "Example", "alice@example.com", "Female",
                        java.time.LocalDate.of(2000, 1, 1), "", "", "", "StrongPass!123"));

        assertEquals(HttpStatus.CONFLICT, exception.status());
    }

    @Test
    void newStudentAccountsStartPendingApproval() {
        AccountRepository accounts = mock(AccountRepository.class);
        AuthService service = new AuthService(accounts, new PasswordHasher());

        AuthService.Account account = service.register(
                "Alice", "Example", "alice@example.com", "Female",
                java.time.LocalDate.of(2000, 1, 1), "", "", "", "StrongPass!123");

        assertEquals("PENDING", account.approvalStatus());
        verify(accounts).create(org.mockito.ArgumentMatchers.eq("Alice"),
                org.mockito.ArgumentMatchers.eq("Example"),
                org.mockito.ArgumentMatchers.eq("alice@example.com"),
                org.mockito.ArgumentMatchers.eq("Female"),
                org.mockito.ArgumentMatchers.eq(java.time.LocalDate.of(2000, 1, 1)),
                org.mockito.ArgumentMatchers.eq(""), org.mockito.ArgumentMatchers.eq(""),
                org.mockito.ArgumentMatchers.eq(""),
                org.mockito.ArgumentMatchers.any(PasswordHasher.HashedPassword.class));
    }

    @Test
    void blocksLoginUntilApplicationIsAccepted() {
        AccountRepository accounts = mock(AccountRepository.class);
        PasswordHasher passwords = mock(PasswordHasher.class);
        when(accounts.findByEmail("alice@example.com")).thenReturn(Optional.of(
                new AccountRepository.AccountRecord(1L, "Alice", "Example", "alice@example.com",
                        "STUDENT", "PENDING", "Female", "2000-01-01", "", "", "",
                        null, "salt", "hash")));
        when(passwords.matches(org.mockito.ArgumentMatchers.any(char[].class),
                org.mockito.ArgumentMatchers.eq("salt"), org.mockito.ArgumentMatchers.eq("hash")))
                .thenReturn(true);
        AuthService service = new AuthService(accounts, passwords);

        AuthException exception = assertThrows(AuthException.class,
                () -> service.authenticate("alice@example.com", "StrongPass!123"));

        assertEquals(HttpStatus.FORBIDDEN, exception.status());
        assertEquals("Your account is awaiting administrator approval.", exception.getMessage());
    }

    @Test
    void acceptsOnlyPendingStudentApplications() {
        AccountRepository accounts = mock(AccountRepository.class);
        when(accounts.findByEmail("alice@example.com")).thenReturn(Optional.of(
                new AccountRepository.AccountRecord(1L, "Alice", "Example", "alice@example.com",
                        "STUDENT", "PENDING", "Female", "2000-01-01", "", "", "",
                        null, "salt", "hash")));
        when(accounts.updatePendingStudentApproval("alice@example.com", "ACCEPTED")).thenReturn(1);
        AuthService service = new AuthService(accounts, new PasswordHasher());

        service.updateStudentApproval("ALICE@example.com", "ACCEPTED");

        verify(accounts).updatePendingStudentApproval("alice@example.com", "ACCEPTED");
    }

    @Test
    void returnsAcceptedStatusAfterSuccessfulLogin() {
        AccountRepository accounts = mock(AccountRepository.class);
        PasswordHasher passwords = mock(PasswordHasher.class);
        when(accounts.findByEmail("alice@example.com")).thenReturn(Optional.of(
                new AccountRepository.AccountRecord(1L, "Alice", "Example", "alice@example.com",
                        "STUDENT", "ACCEPTED", "Female", "2000-01-01", "", "", "",
                        "STU-2026-000001", "salt", "hash")));
        when(passwords.matches(org.mockito.ArgumentMatchers.any(char[].class),
                org.mockito.ArgumentMatchers.eq("salt"), org.mockito.ArgumentMatchers.eq("hash")))
                .thenReturn(true);
        AuthService service = new AuthService(accounts, passwords);

        AuthService.Account account = service.authenticate("alice@example.com", "StrongPass!123");

        assertEquals("ACCEPTED", account.approvalStatus());
    }
}
