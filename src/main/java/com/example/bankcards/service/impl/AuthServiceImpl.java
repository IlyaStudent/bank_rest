package com.example.bankcards.service.impl;

import com.example.bankcards.dto.authentication.AuthResponse;
import com.example.bankcards.dto.authentication.LoginRequest;
import com.example.bankcards.dto.authentication.RefreshRequest;
import com.example.bankcards.dto.authentication.RegisterRequest;
import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AuthException;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ResourceExistsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.RefreshTokenRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtProvider;
import com.example.bankcards.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse register(RegisterRequest registerRequest) {
        log.debug("Registration attempt: username='{}'", registerRequest.getUsername());

        validateRegistration(registerRequest);
        User user = createUser(registerRequest);

        log.info("User registered successfully: username='{}', id={}", user.getUsername(), user.getId());

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        log.debug("Login attempt: username='{}'", loginRequest.getUsername());

        User user = authenticate(loginRequest);

        log.info("User logged in successfully: username='{}', id={}", user.getUsername(), user.getId());

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refresh(RefreshRequest refreshRequest) {
        RefreshToken stored = validateAndRevokeRefreshToken(refreshRequest.getRefreshToken());
        return buildAuthResponse(stored.getUser());
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(this::revokeToken);
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Moscow")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpired(Instant.now());
    }

    // --- Validation --- //

    private void validateRegistration(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw BusinessException.passwordsDoNotMatch();
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw ResourceExistsException.username(request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ResourceExistsException.email(request.getEmail());
        }
    }

    private User authenticate(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(AuthException::invalidCredentials);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw AuthException.invalidCredentials();
        }
        return user;
    }

    // --- User creation --- //

    private User createUser(RegisterRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        Role userRole = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> ResourceNotFoundException.role(RoleType.USER));
        user.getRoles().add(userRole);

        return userRepository.save(user);
    }

    // --- Token management --- //

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken();
        saveRefreshToken(refreshToken, user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProvider.getAccessTokenExpiration())
                .user(userMapper.toDto(user))
                .build();
    }

    private RefreshToken validateAndRevokeRefreshToken(String token) {
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(AuthException::invalidRefreshToken);

        if (stored.getRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw AuthException.invalidRefreshToken();
        }

        revokeToken(stored);
        return stored;
    }

    private void saveRefreshToken(String token, User user) {
        RefreshToken refreshEntity = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshEntity);
    }

    private void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }
}
