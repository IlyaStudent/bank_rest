package com.example.bankcards.controller;

import com.example.bankcards.dto.authentication.AuthResponse;
import com.example.bankcards.dto.authentication.LoginRequest;
import com.example.bankcards.dto.authentication.RegisterRequest;
import com.example.bankcards.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "User registration and authentication")
@RestController
@RequiredArgsConstructor
@RequestMapping("${end.point.auth}")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register new user", description = "Creates a new user account and returns JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists"),
            @ApiResponse(responseCode = "422", description = "Passwords do not match")
    })
    @PostMapping("${end.point.register}")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        return authService.register(registerRequest);
    }

    @Operation(summary = "Login", description = "Authenticates user and returns JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("${end.point.login}")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        return authService.login(loginRequest);
    }
}
