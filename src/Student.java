public class Student {
    private final String id;
    private String name;
    private String surname;
    private String email;
    private String course;
    private String phone;
    private String studentId;
    private String status;

    public Student(String id, String name, String email, String course, String phone) {
        this(id, name, "", email, course, phone, "");
    }

    public Student(String id, String name, String surname, String email, String course, String phone) {
        this(id, name, surname, email, course, phone, "");
    }

    public Student(String id, String name, String surname, String email, String course,
                   String phone, String studentId) {
        this.id = id;
        this.name = name;
        this.surname = surname == null ? "" : surname;
        this.email = email;
        this.course = course;
        this.phone = phone;
        this.studentId = studentId == null ? "" : studentId;
        this.status = studentId == null || studentId.isBlank() ? "PENDING" : "ADMITTED";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname == null ? "" : surname;
    }

    public String getEmail() {
        return email;
    }

    public String getCourse() {
        return course;
    }

    public String getPhone() {
        return phone;
    }

    public String getStudentId() {
        return studentId == null ? "" : studentId;
    }

    public String getStatus() {
        return status == null ? "PENDING" : status;
    }

    public void setSurname(String surname) {
        this.surname = surname == null ? "" : surname;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId == null ? "" : studentId;
        this.status = this.studentId.isBlank() ? "PENDING" : "ADMITTED";
    }

    public void setStatus(String status) {
        this.status = status == null ? "PENDING" : status;
    }

    public void updateDetails(String name, String email, String course, String phone) {
        this.name = name;
        this.email = email;
        this.course = course;
        this.phone = phone;
    }
}
