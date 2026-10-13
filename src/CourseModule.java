import java.math.BigDecimal;

public record CourseModule(
        String faculty,
        String courseCode,
        String courseName,
        String moduleCode,
        String moduleName,
        int credits,
        BigDecimal priceZar) {}
