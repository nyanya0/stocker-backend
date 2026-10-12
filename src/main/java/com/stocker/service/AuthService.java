package com.stocker.service;

import com.stocker.dto.Dtos.AuthResponse;
import com.stocker.dto.Dtos.LoginRequest;
import com.stocker.dto.Dtos.RegisterRequest;
import com.stocker.dto.Dtos.UpdateProfileRequest;
import com.stocker.exception.ApiException;
import com.stocker.model.Portfolio;
import com.stocker.model.User;
import com.stocker.repository.PortfolioRepository;
import com.stocker.repository.UserRepository;
import com.stocker.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UserRepository users;
    private final PortfolioRepository portfolios;
    private final JwtService jwt;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository users, PortfolioRepository portfolios, JwtService jwt) {
        this.users = users;
        this.portfolios = portfolios;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req == null) {
            throw ApiException.badRequest("Name, email and a password of at least 6 characters are required");
        }
        String name = clean(req.name());
        String email = clean(req.email()).toLowerCase();
        String password = req.password();

        if (name.isEmpty() || email.isEmpty() || !isValidPassword(password)) {
            throw ApiException.badRequest("Name, email and a password of at least 6 characters are required");
        }
        if (users.existsByEmail(email)) {
            throw ApiException.badRequest("User already exists");
        }

        User user = createUser(name, email, password, "USER");
        return AuthResponse.of(user, jwt.generate(user.getId()));
    }

    /** Creates a user together with their empty portfolio. */
    @Transactional
    public User createUser(String name, String email, String rawPassword, String role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(encoder.encode(rawPassword));
        user.setRole(role);
        user = users.save(user);

        Portfolio portfolio = new Portfolio();
        portfolio.setUser(user);
        portfolios.save(portfolio);
        return user;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        if (req == null) {
            throw ApiException.badRequest("Email and password are required");
        }
        String email = clean(req.email()).toLowerCase();
        User user = users.findByEmail(email).orElse(null);
        if (user == null || req.password() == null || !encoder.matches(req.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return AuthResponse.of(user, jwt.generate(user.getId()));
    }

    @Transactional(readOnly = true)
    public AuthResponse profile(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        return AuthResponse.of(user, null);
    }

    @Transactional
    public AuthResponse updateProfile(Long userId, UpdateProfileRequest req) {
        if (req == null) {
            throw ApiException.badRequest("Profile update details are required");
        }
        if (req.password() != null && !req.password().isEmpty() && !isValidPassword(req.password())) {
            throw ApiException.badRequest("Password must be at least 6 characters and no more than 72 UTF-8 bytes");
        }
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));

        if (req.name() != null && !req.name().isBlank()) {
            user.setName(req.name().trim());
        }
        if (req.email() != null && !req.email().isBlank()) {
            String email = req.email().trim().toLowerCase();
            if (!email.equals(user.getEmail()) && users.existsByEmail(email)) {
                throw ApiException.badRequest("That email is already in use");
            }
            user.setEmail(email);
        }
        if (req.password() != null && !req.password().isEmpty()) {
            user.setPassword(encoder.encode(req.password()));
        }
        users.save(user);
        return AuthResponse.of(user, jwt.generate(user.getId()));
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isValidPassword(String password) {
        return password != null
                && password.length() >= 6
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BCRYPT_PASSWORD_BYTES;
    }
}
