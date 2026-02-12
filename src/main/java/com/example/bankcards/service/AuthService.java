package com.example.bankcards.service;

import com.example.bankcards.dto.authentication.AuthResponse;
import com.example.bankcards.dto.authentication.LoginRequest;
import com.example.bankcards.dto.authentication.RefreshRequest;
import com.example.bankcards.dto.authentication.RegisterRequest;
import jakarta.validation.constraints.NotNull;

public interface AuthService {
    AuthResponse register(@NotNull RegisterRequest registerRequest);

    AuthResponse login(@NotNull LoginRequest loginRequest);

    AuthResponse refresh(@NotNull RefreshRequest refreshRequest);

    void logout(String refreshToken, String accessToken);
}
