package com.studenthub.backend.api;

import com.studenthub.backend.course.CourseCatalogRepository;
import com.studenthub.backend.course.CourseCatalogRepository.CourseModule;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CourseCatalogController {
    private final CourseCatalogRepository catalog;

    public CourseCatalogController(CourseCatalogRepository catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/modules")
    public List<CourseModule> modules() {
        return catalog.findAllModules();
    }
}
