CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    role VARCHAR(16) NOT NULL DEFAULT 'STUDENT',
    approval_status VARCHAR(16) NOT NULL DEFAULT 'ACCEPTED',
    surname VARCHAR(100) NOT NULL DEFAULT '',
    gender VARCHAR(32) NOT NULL DEFAULT '',
    date_of_birth DATE NULL,
    phone VARCHAR(40) NOT NULL DEFAULT '',
    home_address VARCHAR(500) NOT NULL DEFAULT '',
    course_interest VARCHAR(150) NOT NULL DEFAULT '',
    student_id VARCHAR(32) NULL,
    password_salt CHAR(32) NOT NULL,
    password_hash CHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_accounts_student_id (student_id),
    CONSTRAINT chk_account_approval_status
        CHECK (approval_status IN ('PENDING', 'ACCEPTED', 'DECLINED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS students (
    student_id VARCHAR(64) NOT NULL PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(254) NOT NULL DEFAULT '',
    course VARCHAR(150) NOT NULL DEFAULT '',
    phone VARCHAR(40) NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS courses (
    course_code VARCHAR(32) NOT NULL PRIMARY KEY,
    faculty VARCHAR(120) NOT NULL,
    course_name VARCHAR(200) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course_modules (
    module_code VARCHAR(32) NOT NULL PRIMARY KEY,
    course_code VARCHAR(32) NOT NULL,
    module_name VARCHAR(200) NOT NULL,
    credits SMALLINT UNSIGNED NOT NULL,
    price_zar DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_module_course FOREIGN KEY (course_code)
        REFERENCES courses(course_code) ON DELETE CASCADE,
    CONSTRAINT chk_module_price CHECK (price_zar >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS enrollments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_email VARCHAR(254) NOT NULL,
    module_code VARCHAR(32) NOT NULL,
    price_at_enrolment DECIMAL(10, 2) NOT NULL,
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_account_module UNIQUE (account_email, module_code),
    CONSTRAINT fk_enrollment_account FOREIGN KEY (account_email)
        REFERENCES accounts(email) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_module FOREIGN KEY (module_code)
        REFERENCES course_modules(module_code),
    CONSTRAINT chk_enrollment_price CHECK (price_at_enrolment >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
