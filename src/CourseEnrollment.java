import java.math.BigDecimal;

public record CourseEnrollment(
        String enrollmentId,
        String accountEmail,
        String faculty,
        String courseCode,
        String courseName,
        String moduleCode,
        String moduleName,
        int credits,
        BigDecimal priceZar,
        String enrolledAt) {}
