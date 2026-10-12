package com.stocker.service;

import com.stocker.dto.Dtos.RegisterRequest;
import com.stocker.dto.Dtos.UpdateProfileRequest;
import com.stocker.exception.ApiException;
import com.stocker.repository.PortfolioRepository;
import com.stocker.repository.UserRepository;
import com.stocker.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AuthServiceTest {

    private final AuthService auth = new AuthService(
            mock(UserRepository.class),
            mock(PortfolioRepository.class),
            mock(JwtService.class));

    @Test
    void registerRejectsNullRequestAsBadRequest() {
        ApiException exception = assertThrows(ApiException.class, () -> auth.register(null));

        assertEquals(400, exception.getStatus().value());
    }

    @Test
    void loginRejectsNullRequestAsBadRequest() {
        ApiException exception = assertThrows(ApiException.class, () -> auth.login(null));

        assertEquals(400, exception.getStatus().value());
    }

    @Test
    void updateProfileRejectsNullRequestAsBadRequest() {
        ApiException exception = assertThrows(ApiException.class, () -> auth.updateProfile(1L, null));

        assertEquals(400, exception.getStatus().value());
    }

    @Test
    void registerRejectsPasswordsLongerThanBcryptLimit() {
        String password = "a".repeat(73);
        RegisterRequest request = new RegisterRequest("Test", "test@example.com", password);

        ApiException exception = assertThrows(ApiException.class, () -> auth.register(request));

        assertEquals(400, exception.getStatus().value());
    }

    @Test
    void updateProfileRejectsPasswordsLongerThanBcryptLimit() {
        String password = "a".repeat(73);
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, password);

        ApiException exception = assertThrows(ApiException.class, () -> auth.updateProfile(1L, request));

        assertEquals(400, exception.getStatus().value());
    }
}
