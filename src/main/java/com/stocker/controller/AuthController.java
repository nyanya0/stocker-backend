package com.stocker.controller;

import com.stocker.dto.Dtos.AuthResponse;
import com.stocker.dto.Dtos.LoginRequest;
import com.stocker.dto.Dtos.RegisterRequest;
import com.stocker.dto.Dtos.UpdateProfileRequest;
import com.stocker.model.User;
import com.stocker.security.AuthInterceptor;
import com.stocker.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @GetMapping("/profile")
    public AuthResponse profile(@RequestAttribute(AuthInterceptor.CURRENT_USER) User current) {
        return auth.profile(current.getId());
    }

    @PutMapping("/profile")
    public AuthResponse updateProfile(@RequestAttribute(AuthInterceptor.CURRENT_USER) User current,
                                      @RequestBody UpdateProfileRequest request) {
        return auth.updateProfile(current.getId(), request);
    }
}
