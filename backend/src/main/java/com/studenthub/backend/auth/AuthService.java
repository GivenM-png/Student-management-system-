package com.studenthub.backend.auth;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$");

    private final AccountRepository accounts;
    private final PasswordHasher passwords;

    public AuthService(AccountRepository accounts, PasswordHasher passwords) {
        this.accounts = accounts;
        this.passwords = passwords;
    }

    public Account register(String name, String surname, String email, String gender,
                            java.time.LocalDate birthDate, String phone, String address,
                            String courseInterest, String password) {
        String normalizedName = validateName(name);
        String normalizedSurname = validateSurname(surname);
        String normalizedEmail = validateEmail(email);
        String normalizedGender = validateGender(gender);
        if (birthDate == null || !birthDate.isBefore(java.time.LocalDate.now())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Enter a valid date of birth in the past.");
        }
        String normalizedPhone = optionalField(phone, 40, "Phone number");
        String normalizedAddress = optionalField(address, 500, "Home address");
        String normalizedCourseInterest = optionalField(courseInterest, 150, "Programme of interest");
        validatePassword(password);
        if (accounts.findByEmail(normalizedEmail).isPresent()) {
            throw new AuthException(HttpStatus.CONFLICT, "An account already exists for that email.");
        }

        char[] secret = password.toCharArray();
        PasswordHasher.HashedPassword hashed;
        try {
            hashed = passwords.hash(secret);
        } finally {
            java.util.Arrays.fill(secret, '\0');
        }
        accounts.create(normalizedName, normalizedSurname, normalizedEmail, normalizedGender, birthDate,
                normalizedPhone, normalizedAddress, normalizedCourseInterest, hashed);
        return new Account(normalizedName, normalizedSurname, normalizedEmail, "STUDENT", "PENDING",
                normalizedGender, birthDate.toString(), normalizedPhone, normalizedAddress,
                normalizedCourseInterest, "");
    }

    public Account authenticate(String email, String password) {
        String normalizedEmail = validateEmail(email);
        char[] secret = password == null ? new char[0] : password.toCharArray();
        try {
            AccountRepository.AccountRecord record = accounts.findByEmail(normalizedEmail).orElse(null);
            if (record == null) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
            }
            if (!passwords.matches(secret, record.salt(), record.hash())) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
            }
            if ("PENDING".equals(record.approvalStatus())) {
                throw new AuthException(HttpStatus.FORBIDDEN,
                        "Your account is awaiting administrator approval.");
            }
            if ("DECLINED".equals(record.approvalStatus())) {
                throw new AuthException(HttpStatus.FORBIDDEN,
                        "Your account application was declined. Contact your administrator.");
            }
            return account(record);
        } finally {
            java.util.Arrays.fill(secret, '\0');
        }
    }

    public Account findAccount(String email) {
        AccountRepository.AccountRecord record = accounts.findByEmail(normalize(email))
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED,
                        "This account is no longer available."));
        return account(record);
    }

    private Account account(AccountRepository.AccountRecord record) {
        return new Account(record.name(), record.surname(), record.email(), record.role(),
                record.approvalStatus(), record.gender(), record.birthDate(), record.phone(),
                record.address(), record.courseInterest(), record.studentId());
    }

    public java.util.List<AccountRepository.StudentApplication> findStudentApplications() {
        return accounts.findStudentApplications();
    }

    public void updateStudentApproval(String email, String approvalStatus) {
        String normalizedEmail = normalize(email);
        if (!"ACCEPTED".equals(approvalStatus) && !"DECLINED".equals(approvalStatus)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Approval status must be ACCEPTED or DECLINED.");
        }
        AccountRepository.AccountRecord record = accounts.findByEmail(normalizedEmail)
                .filter(account -> "STUDENT".equals(account.role()))
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Student application not found."));
        if (!"PENDING".equals(record.approvalStatus())) {
            throw new AuthException(HttpStatus.CONFLICT, "This student application has already been reviewed.");
        }
        if (accounts.updatePendingStudentApproval(normalizedEmail, approvalStatus) != 1) {
            throw new AuthException(HttpStatus.CONFLICT, "This student application has already been reviewed.");
        }
    }

    public void ensureAdmin(String name, String email, String password) {
        String normalizedName = validateName(name);
        String normalizedEmail = validateEmail(email);
        validatePassword(password);

        char[] secret = password.toCharArray();
        PasswordHasher.HashedPassword hashed;
        try {
            hashed = passwords.hash(secret);
        } finally {
            java.util.Arrays.fill(secret, '\0');
        }
        accounts.provisionAdmin(normalizedName, normalizedEmail, hashed);
    }

    private String validateName(String name) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isBlank() || normalizedName.length() > 200) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Name is required.");
        }
        return normalizedName;
    }

    private String validateSurname(String surname) {
        String normalized = surname == null ? "" : surname.trim();
        if (normalized.isBlank() || normalized.length() > 100) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Surname is required.");
        }
        return normalized;
    }

    private String validateGender(String gender) {
        String normalized = gender == null ? "" : gender.trim();
        if (!java.util.Set.of("Female", "Male", "Non-binary", "Prefer not to say").contains(normalized)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Choose a valid gender option.");
        }
        return normalized;
    }

    private String optionalField(String value, int maxLength, String label) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) {
            throw new AuthException(HttpStatus.BAD_REQUEST, label + " is too long.");
        }
        return normalized;
    }

    private String validateEmail(String email) {
        String normalizedEmail = normalize(email);
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Enter a valid email address.");
        }
        return normalizedEmail;
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "Password is required.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new AuthException(HttpStatus.BAD_REQUEST,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }
        if (password.isBlank() || password.chars().anyMatch(Character::isWhitespace)) {
            throw new AuthException(HttpStatus.BAD_REQUEST,
                    "Password must not contain spaces or blank values.");
        }
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSymbol = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!(hasLower && hasUpper && hasDigit && hasSymbol)) {
            throw new AuthException(HttpStatus.BAD_REQUEST,
                    "Password must include upper and lower case letters, a number, and a symbol.");
        }
    }

    private String normalize(String email) {
        return Objects.requireNonNullElse(email, "").trim().toLowerCase(Locale.ROOT);
    }

    public record Account(String name, String surname, String email, String role, String approvalStatus,
                         String gender, String birthDate, String phone, String address,
                         String courseInterest, String studentId) {}
}
