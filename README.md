# StudentHub backend (MySQL)

The Swing application uses a Spring Boot REST API. The API connects to MySQL; the desktop app does not connect to the database directly.

## Requirements

- JDK 17 or newer
- Apache Maven 3.6.3 or newer
- MySQL 8.0+

MySQL Workbench is only a database management interface; install and start **MySQL Server** as well.

## One-time MySQL setup

MySQL Server 8.4 must be installed. The included `run-mysql.bat` starts the StudentHub-only data directory at `%LOCALAPPDATA%\StudentHub\MySQLData`, binds it to `127.0.0.1:3306`, and disables the unused MySQL X Protocol; it does not use the older protected MySQL data directory. The API also binds to `127.0.0.1:8080` by default.

The database, restricted application user, and random credentials have been initialized locally in the ignored `backend\.env` file. Keep that file private and do not commit it. It also contains the bootstrapped administrator's email and generated password. On startup, the backend creates the account, student, course, module, and enrolment tables, migrates existing accounts to the `STUDENT` role, bootstraps the configured `ADMIN` account, and seeds an editable catalogue of indicative sample offerings. Optional settings are `MYSQL_URL`, `MYSQL_USER`, and `SERVER_PORT`.

Public sign-up has two steps: account credentials, followed by a student profile with name, surname, gender, date of birth, and optional contact, address, and programme details. Submitting creates a `STUDENT` account in `PENDING` approval status; it cannot grant administrator access. Pending students cannot sign in until an administrator accepts their application. Administrators review registered profiles on the dashboard and can accept or decline each pending application. Acceptance assigns a permanent student ID, which appears on the student's dashboard after sign-in. Declined students see a rejection message when they try to sign in. Existing accounts are migrated as accepted to preserve access and receive a student ID.

The administrator is provisioned only from the private `ADMIN_NAME`, `ADMIN_EMAIL`, and `ADMIN_PASSWORD` settings in `backend\.env`. To provision another administrator, set a new unique email and a password of at least 16 characters there before starting the backend. The values in that file are authoritative: on backend start, the configured administrator account is assigned `ADMIN` and its password is reset to the configured value. The app database user requires `CREATE`, `ALTER`, and `REFERENCES` privileges on the `studenthub` schema for startup schema management.

Start `run-mysql.bat` and keep its console open. In another window run `run-backend.bat`; then start the Swing app with `run.bat`. Restart the backend after updating it so its startup migration adds profile fields and student IDs to existing accounts. It connects to `http://127.0.0.1:8080` by default; set `STUDENTHUB_API_URL` if the backend runs elsewhere. Student accounts, profiles, approval decisions, assigned IDs, and module enrolments are stored in MySQL. Passwords are salted PBKDF2 hashes. API access tokens expire after 12 hours and are invalidated when the backend restarts.

## Course catalogue and enrolment

Students get a workspace for their registered profile, the catalogue, and their own enrolments. Administrators review registered student profiles and accept or decline applications on the dashboard, manage indicative module fees, and see all-student enrolment oversight. Administrators cannot manually add student records. Role checks are enforced by the API as well as the UI. The starter catalogue contains 22 representative example programmes and 198 sample modules across engineering, health sciences, computer science, data science, information technology, and Economic and Management Sciences. The sample modules have indicative ZAR fees only; they are not an exhaustive or officially verified curriculum or fee schedule. Confirm and update these records for the actual institution before relying on them. The Swing catalogue highlights each module fee, supports faculty/search filtering, allows a student to enrol or withdraw, and shows that student's enrolment total. Enrolment does not take payment.

Edit fees and catalogue entries in MySQL Workbench by updating `course_modules.price_zar` or editing `courses` and `course_modules`. The seed uses `INSERT IGNORE`, so existing catalogue edits are preserved across backend restarts.

The default HTTP configuration is for local development. Do not expose the backend on a network without putting it behind HTTPS.

## API

- `POST /api/auth/register` creates a pending student account; `POST /api/auth/login` issues a bearer token after approval; `POST /api/auth/logout` revokes one.
- `GET /api/admin/applications` lists student accounts and their approval status; `PUT /api/admin/applications/{email}/approval` accepts or declines a pending application with `{"status":"ACCEPTED"}` or `{"status":"DECLINED"}`.
- There is no admin endpoint for manually creating student records; students apply through account registration, and acceptance assigns their student ID.
- `GET /api/catalog/modules` lists priced modules; `GET /api/enrollments` lists the authenticated student's enrolments, `POST /api/enrollments` enrols in a module, and `DELETE /api/enrollments/{id}` withdraws from it.
- `GET /api/admin/enrollments` provides the administrator's all-student overview; `PUT /api/admin/modules/{moduleCode}/price` updates an indicative module fee.

When manually creating the restricted MySQL account, grant `SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, REFERENCES` on `studenthub.*`; the application schema uses foreign keys between accounts, courses, modules, and enrolments.
