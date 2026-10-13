package com.studenthub.backend.config;

import com.studenthub.backend.auth.TokenAuthInterceptor;
import com.studenthub.backend.auth.AdminOnlyInterceptor;
import com.studenthub.backend.auth.StudentOnlyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final TokenAuthInterceptor tokenAuthInterceptor;
    private final AdminOnlyInterceptor adminOnlyInterceptor;
    private final StudentOnlyInterceptor studentOnlyInterceptor;

    public WebConfig(TokenAuthInterceptor tokenAuthInterceptor, AdminOnlyInterceptor adminOnlyInterceptor,
                     StudentOnlyInterceptor studentOnlyInterceptor) {
        this.tokenAuthInterceptor = tokenAuthInterceptor;
        this.adminOnlyInterceptor = adminOnlyInterceptor;
        this.studentOnlyInterceptor = studentOnlyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenAuthInterceptor).addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/register", "/api/auth/login", "/api/catalog/modules");
        registry.addInterceptor(adminOnlyInterceptor)
                .addPathPatterns("/api/students/**", "/api/admin/**", "/api/catalog/admin/**");
        registry.addInterceptor(studentOnlyInterceptor)
                .addPathPatterns("/api/enrollments", "/api/enrollments/**");
    }
}
