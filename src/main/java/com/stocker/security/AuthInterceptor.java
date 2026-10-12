package com.stocker.security;

import com.stocker.exception.ApiException;
import com.stocker.model.User;
import com.stocker.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Checks the "Authorization: Bearer <token>" header on protected routes (see WebConfig)
 * and stores the logged-in user in the request as "currentUser".
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER = "currentUser";

    private final JwtService jwtService;
    private final UserRepository users;

    public AuthInterceptor(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS pre-flight requests carry no token - let them through
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authorized, no token");
        }

        Long userId;
        try {
            userId = jwtService.parseUserId(header.substring(7));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authorized, token failed");
        }

        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authorized, user no longer exists"));

        String path = request.getRequestURI();
        boolean adminOnly = path.startsWith("/api/admin") || path.equals("/api/stocks/seed");
        if (adminOnly && !"ADMIN".equals(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not authorized as an admin");
        }

        request.setAttribute(CURRENT_USER, user);
        return true;
    }
}
