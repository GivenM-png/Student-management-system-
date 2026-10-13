import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

public class ApiClient {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final String baseUrl = System.getenv().getOrDefault(
            "STUDENTHUB_API_URL", "http://127.0.0.1:8080").replaceAll("/+$", "");
    private String token;
    private String accountName = "";
    private String accountSurname = "";
    private String accountEmail = "";
    private String accountRole = "";
    private String accountApprovalStatus = "";
    private String accountGender = "";
    private String accountBirthDate = "";
    private String accountPhone = "";
    private String accountAddress = "";
    private String accountCourseInterest = "";
    private String accountStudentId = "";

    public void login(String email, char[] password) {
        try {
            String response = send("POST", "/api/auth/login",
                    "{\"email\":" + quote(email) + ",\"password\":" + quote(new String(password)) + "}",
                    false);
            setSession(JsonCodec.object(response));
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    public void register(String name, String surname, String email, String gender, String birthDate,
                         String phone, String address, String courseInterest, char[] password) {
        try {
            send("POST", "/api/auth/register",
                    "{\"name\":" + quote(name) + ",\"surname\":" + quote(surname)
                            + ",\"email\":" + quote(email) + ",\"gender\":" + quote(gender)
                            + ",\"birthDate\":" + quote(birthDate) + ",\"phone\":" + quote(phone)
                            + ",\"address\":" + quote(address) + ",\"courseInterest\":" + quote(courseInterest)
                            + ",\"password\":" + quote(new String(password)) + "}", false);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    public void logout() {
        if (token == null) {
            return;
        }
        try {
            send("POST", "/api/auth/logout", null, true);
        } finally {
            clearSession();
        }
    }

    public void clearSession() {
        token = null;
        accountName = "";
        accountSurname = "";
        accountEmail = "";
        accountRole = "";
        accountApprovalStatus = "";
        accountGender = "";
        accountBirthDate = "";
        accountPhone = "";
        accountAddress = "";
        accountCourseInterest = "";
        accountStudentId = "";
    }

    public String accountName() {
        return accountName;
    }

    public String accountRole() {
        return accountRole;
    }

    public String accountSurname() { return accountSurname; }
    public String accountEmail() { return accountEmail; }
    public String accountGender() { return accountGender; }
    public String accountBirthDate() { return accountBirthDate; }
    public String accountPhone() { return accountPhone; }
    public String accountAddress() { return accountAddress; }
    public String accountCourseInterest() { return accountCourseInterest; }
    public String accountStudentId() { return accountStudentId; }

    public String accountApprovalStatus() {
        return accountApprovalStatus;
    }

    public List<Student> getStudents() {
        return JsonCodec.students(send("GET", "/api/students", null, true));
    }

    public void updateStudent(Student student) {
        send("PUT", "/api/students/" + encodePath(student.getId()), studentJson(student), true);
    }

    public void deleteStudent(String id) {
        send("DELETE", "/api/students/" + encodePath(id), null, true);
    }

    public List<CourseModule> getCourseModules() {
        return JsonCodec.modules(send("GET", "/api/catalog/modules", null, true));
    }

    public List<CourseModule> getAvailableCourseModules() {
        return JsonCodec.modules(send("GET", "/api/catalog/modules", null, false));
    }

    public List<CourseEnrollment> getMyEnrollments() {
        return JsonCodec.enrollments(send("GET", "/api/enrollments", null, true));
    }

    public void enrollInModule(String moduleCode) {
        send("POST", "/api/enrollments", "{\"moduleCode\":" + quote(moduleCode) + "}", true);
    }

    public void withdrawFromModule(String enrollmentId) {
        send("DELETE", "/api/enrollments/" + encodePath(enrollmentId), null, true);
    }

    public List<CourseEnrollment> getAllEnrollments() {
        return JsonCodec.enrollments(send("GET", "/api/admin/enrollments", null, true));
    }

    public List<StudentApplication> getStudentApplications() {
        return JsonCodec.applications(send("GET", "/api/admin/applications", null, true));
    }

    public void updateStudentApplication(String email, String status) {
        send("PUT", "/api/admin/applications/" + encodePath(email) + "/approval",
                "{\"status\":" + quote(status) + "}", true);
    }

    public AdminOverview getAdminOverview() {
        return JsonCodec.overview(send("GET", "/api/admin/overview", null, true));
    }

    public String exportStudentsCsv() {
        return send("GET", "/api/admin/export/students", null, true, "text/csv");
    }

    public String exportEnrollmentsCsv() {
        return send("GET", "/api/admin/export/enrollments", null, true, "text/csv");
    }

    public void updateModulePrice(String moduleCode, BigDecimal priceZar) {
        send("PUT", "/api/admin/modules/" + encodePath(moduleCode) + "/price",
                "{\"priceZar\":" + priceZar.toPlainString() + "}", true);
    }

    private void setSession(LinkedHashMap<String, String> response) {
        token = response.get("token");
        accountName = response.get("name");
        accountSurname = optional(response.get("surname"));
        accountEmail = response.get("email");
        accountRole = response.get("role");
        accountApprovalStatus = response.get("approvalStatus");
        accountGender = optional(response.get("gender"));
        accountBirthDate = optional(response.get("birthDate"));
        accountPhone = optional(response.get("phone"));
        accountAddress = optional(response.get("address"));
        accountCourseInterest = optional(response.get("courseInterest"));
        accountStudentId = optional(response.get("studentId"));
        if (token == null || accountName == null || accountSurname == null || accountEmail == null
                || accountRole == null || accountApprovalStatus == null) {
            clearSession();
            throw new ApiException("The server returned an incomplete sign-in response.");
        }
    }

    private String optional(String value) {
        return value == null || value.equals("null") ? "" : value;
    }

    private String studentJson(Student student) {
        return "{\"id\":" + quote(student.getId()) + ",\"name\":" + quote(student.getName())
                + ",\"email\":" + quote(student.getEmail()) + ",\"course\":"
                + quote(student.getCourse()) + ",\"phone\":" + quote(student.getPhone()) + "}";
    }

    private String send(String method, String path, String body, boolean authenticated) {
        return send(method, path, body, authenticated, "application/json");
    }

    private String send(String method, String path, String body, boolean authenticated, String acceptType) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", acceptType);
        if (body != null) {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        }
        if (authenticated) {
            if (token == null) {
                throw new ApiException("Sign in to continue.");
            }
            request.header("Authorization", "Bearer " + token);
        }

        try {
            HttpResponse<String> response = client.send(request.build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = JsonCodec.errorMessage(response.body());
                throw new ApiException(message == null
                        ? "The server returned an error (" + response.statusCode() + ")."
                        : message);
            }
            return response.body();
        } catch (IOException e) {
            throw new ApiException("Cannot reach StudentHub backend at " + baseUrl
                    + ". Start the backend and try again.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("The request was interrupted.", e);
        }
    }

    private long parseLong(String value, long fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    public record AdminOverview(long studentCount, long accountCount, long enrollmentCount,
                                long moduleCount, long profileCount,
                                List<RecentActivity> recentActivity) {}

    public record RecentActivity(String createdAt, String actorEmail, String actorRole,
                                 String action, String entityType, String entityId,
                                 String details) {}

    public record StudentApplication(String email, String name, String surname, String gender,
                                     String birthDate, String phone, String address, String courseInterest,
                                     String approvalStatus, String studentId) {}

    private String encodePath(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    public static class ApiException extends RuntimeException {
        public ApiException(String message) {
            super(message);
        }

        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class JsonCodec {
        static LinkedHashMap<String, String> object(String json) {
            Parser parser = new Parser(json);
            return parser.readObject();
        }

        static List<Student> students(String json) {
            List<Student> students = new ArrayList<>();
            for (LinkedHashMap<String, String> value : objects(json)) {
                students.add(new Student(value.get("id"), value.get("name"), value.get("email"),
                        value.get("course"), value.get("phone")));
            }
            return students;
        }

        static List<CourseModule> modules(String json) {
            List<CourseModule> modules = new ArrayList<>();
            for (LinkedHashMap<String, String> value : objects(json)) {
                modules.add(new CourseModule(value.get("faculty"), value.get("courseCode"),
                        value.get("courseName"), value.get("moduleCode"), value.get("moduleName"),
                        Integer.parseInt(value.get("credits")), new BigDecimal(value.get("priceZar"))));
            }
            return modules;
        }

        static List<CourseEnrollment> enrollments(String json) {
            List<CourseEnrollment> enrollments = new ArrayList<>();
            for (LinkedHashMap<String, String> value : objects(json)) {
                enrollments.add(new CourseEnrollment(value.get("enrollmentId"), value.get("accountEmail"),
                        value.get("faculty"), value.get("courseCode"), value.get("courseName"), value.get("moduleCode"),
                        value.get("moduleName"), Integer.parseInt(value.get("credits")),
                        new BigDecimal(value.get("priceZar")), value.get("enrolledAt")));
            }
            return enrollments;
        }

        static List<StudentApplication> applications(String json) {
            List<StudentApplication> applications = new ArrayList<>();
            for (LinkedHashMap<String, String> value : objects(json)) {
                applications.add(new StudentApplication(value.get("email"), value.get("name"),
                        value.get("surname"), value.get("gender"), value.get("birthDate"),
                        value.get("phone"), value.get("address"), value.get("courseInterest"),
                        value.get("approvalStatus"), value.get("studentId")));
            }
            return applications;
        }

        static AdminOverview overview(String json) {
            Parser parser = new Parser(json);
            LinkedHashMap<String, Object> values = parser.readObjectValues();
            List<RecentActivity> recentActivity = new ArrayList<>();
            Object recentValue = values.get("recentActivity");
            if (recentValue instanceof List<?> entries) {
                for (Object entry : entries) {
                    if (entry instanceof LinkedHashMap<?, ?> map) {
                        LinkedHashMap<String, Object> typedMap = new LinkedHashMap<>();
                        for (Map.Entry<?, ?> item : map.entrySet()) {
                            typedMap.put(String.valueOf(item.getKey()), item.getValue());
                        }
                        recentActivity.add(new RecentActivity(
                                asString(typedMap.get("createdAt")),
                                asString(typedMap.get("actorEmail")),
                                asString(typedMap.get("actorRole")),
                                asString(typedMap.get("action")),
                                asString(typedMap.get("entityType")),
                                asString(typedMap.get("entityId")),
                                asString(typedMap.get("details"))));
                    }
                }
            }
            return new AdminOverview(
                    parseLong(asString(values.get("studentCount")), 0L),
                    parseLong(asString(values.get("accountCount")), 0L),
                    parseLong(asString(values.get("enrollmentCount")), 0L),
                    parseLong(asString(values.get("moduleCount")), 0L),
                    parseLong(asString(values.get("profileCount")), 0L),
                    recentActivity);
        }

        private static List<LinkedHashMap<String, String>> objects(String json) {
            Parser parser = new Parser(json);
            List<LinkedHashMap<String, String>> values = new ArrayList<>();
            parser.expect('[');
            if (parser.take(']')) {
                return values;
            }
            do {
                values.add(parser.readObject());
            } while (parser.take(','));
            parser.expect(']');
            return values;
        }

        static String errorMessage(String json) {
            try {
                return object(json).get("message");
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private static String asString(Object value) {
            return value == null ? "" : String.valueOf(value);
        }

        private static long parseLong(String value, long defaultValue) {
            if (value == null || value.isBlank()) {
                return defaultValue;
            }
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        private static class Parser {
            private final String json;
            private int position;

            Parser(String json) {
                this.json = json;
            }

            LinkedHashMap<String, String> readObject() {
                LinkedHashMap<String, String> values = new LinkedHashMap<>();
                expect('{');
                if (take('}')) {
                    return values;
                }
                do {
                    String key = readString();
                    expect(':');
                    values.put(key, readScalar());
                } while (take(','));
                expect('}');
                return values;
            }

            LinkedHashMap<String, Object> readObjectValues() {
                LinkedHashMap<String, Object> values = new LinkedHashMap<>();
                expect('{');
                if (take('}')) {
                    return values;
                }
                do {
                    String key = readString();
                    expect(':');
                    values.put(key, readValue());
                } while (take(','));
                expect('}');
                return values;
            }

            Object readValue() {
                whitespace();
                if (position >= json.length()) {
                    throw new IllegalArgumentException("Unexpected end of JSON");
                }
                char next = json.charAt(position);
                if (next == '"') {
                    return readString();
                }
                if (next == '{') {
                    return readObjectValues();
                }
                if (next == '[') {
                    return readArray();
                }
                return readScalar();
            }

            List<Object> readArray() {
                List<Object> values = new ArrayList<>();
                expect('[');
                if (take(']')) {
                    return values;
                }
                do {
                    values.add(readValue());
                } while (take(','));
                expect(']');
                return values;
            }

            String readString() {
                expect('"');
                StringBuilder value = new StringBuilder();
                while (position < json.length()) {
                    char c = json.charAt(position++);
                    if (c == '"') {
                        return value.toString();
                    }

                    if (c != '\\') {
                        value.append(c);
                        continue;
                    }
                    if (position >= json.length()) {
                        throw new IllegalArgumentException("Invalid JSON escape");
                    }
                    char escape = json.charAt(position++);
                    switch (escape) {
                        case '"', '\\', '/' -> value.append(escape);
                        case 'b' -> value.append('\b');
                        case 'f' -> value.append('\f');
                        case 'n' -> value.append('\n');
                        case 'r' -> value.append('\r');
                        case 't' -> value.append('\t');
                        case 'u' -> {
                            if (position + 4 > json.length()) {
                                throw new IllegalArgumentException("Invalid JSON unicode escape");
                            }
                            value.append((char) Integer.parseInt(json.substring(position, position + 4), 16));
                            position += 4;
                        }
                        default -> throw new IllegalArgumentException("Invalid JSON escape");
                    }
                }
                throw new IllegalArgumentException("Unterminated JSON string");
            }

            String readScalar() {
                whitespace();
                if (position < json.length() && json.charAt(position) == '"') {
                    return readString();
                }
                int start = position;
                while (position < json.length() && json.charAt(position) != ','
                        && json.charAt(position) != '}' && json.charAt(position) != ']' && !Character.isWhitespace(json.charAt(position))) {
                    position++;
                }
                if (position == start) {
                    throw new IllegalArgumentException("Invalid JSON value");
                }
                return json.substring(start, position);
            }

            boolean take(char expected) {
                whitespace();
                if (position < json.length() && json.charAt(position) == expected) {
                    position++;
                    return true;
                }
                return false;
            }

            void expect(char expected) {
                whitespace();
                if (position >= json.length() || json.charAt(position++) != expected) {
                    throw new IllegalArgumentException("Invalid JSON response");
                }
            }

            void whitespace() {
                while (position < json.length() && Character.isWhitespace(json.charAt(position))) {
                    position++;
                }
            }
        }
    }
}
