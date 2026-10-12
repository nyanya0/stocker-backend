package com.stocker.config;

import com.stocker.security.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final String[] allowedOrigins;

    public WebConfig(AuthInterceptor authInterceptor,
                     @Value("${app.cors.allowed-origins}") String origins) {
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = origins.trim().split("\\s*,\\s*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Everything listed here requires a valid login token.
        // Public routes: /api/auth/login, /api/auth/register, GET /api/stocks/**, /api/health
        registry.addInterceptor(authInterceptor).addPathPatterns(
                "/api/auth/profile",
                "/api/users/**",
                "/api/trade/**",
                "/api/portfolio/**",
                "/api/admin/**",
                "/api/chat/**",
                "/api/stocks/seed");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
