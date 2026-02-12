package com.example.bankcards.service.impl;

import com.example.bankcards.dto.authentication.AuthResponse;
import com.example.bankcards.dto.authentication.LoginRequest;
import com.example.bankcards.dto.authentication.RefreshRequest;
import com.example.bankcards.dto.authentication.RegisterRequest;
import com.example.bankcards.dto.user.UserDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;
    private UserDto userDto;
    private Role role;
    private RefreshToken storedToken;
    private String username;
    private String email;
    private String password;
    private String encodedPassword;
    private String accessToken;
    private String refreshToken;
    private Instant expiresIn;

    @BeforeEach
    void setUp() {
        username = "test";
        email = "test@example.com";
        password = "password";
        encodedPassword = "encoded_password";
        accessToken = "jwt_access_token";
        refreshToken = "refresh_token_uuid";
        expiresIn = Instant.ofEpochSecond(3600000L);

        role = Role.builder()
                .id(1L)
                .name(RoleType.USER)
                .build();

        user = User.builder()
                .id(1L)
                .username(username)
                .email(email)
                .password(encodedPassword)
                .roles(new HashSet<>(Set.of(role)))
                .build();

        userDto = UserDto.builder()
                .id(1L)
                .username(username)
                .email(email)
                .roles(Set.of(RoleType.USER.name()))
                .build();

        registerRequest = RegisterRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .confirmPassword(password)
                .build();

        loginRequest = LoginRequest.builder()
                .username(username)
                .password(password)
                .build();

        storedToken = RefreshToken.builder()
                .id(1L)
                .token(refreshToken)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUserSuccessfully() {
            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
            when(roleRepository.findByName(RoleType.USER)).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(jwtProvider.generateAccessToken(any(User.class))).thenReturn(accessToken);
            when(jwtProvider.generateRefreshToken()).thenReturn(refreshToken);
            when(jwtProvider.getAccessTokenExpiration()).thenReturn(expiresIn);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
            when(userMapper.toDto(any(User.class))).thenReturn(userDto);

            AuthResponse result = authService.register(registerRequest);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo(accessToken);
            assertThat(result.getRefreshToken()).isEqualTo(refreshToken);
            assertThat(result.getExpiresIn()).isEqualTo(expiresIn);
            assertThat(result.getUser()).isEqualTo(userDto);

            verify(userRepository).existsByUsername(username);
            verify(userRepository).existsByEmail(email);
            verify(passwordEncoder).encode(password);
            verify(roleRepository).findByName(RoleType.USER);
            verify(userRepository).save(any(User.class));
            verify(jwtProvider).generateAccessToken(any(User.class));
            verify(jwtProvider).generateRefreshToken();
            verify(refreshTokenRepository).save(any(RefreshToken.class));
            verify(userMapper).toDto(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when passwords do not match")
        void shouldThrowExceptionWhenPasswordsDoNotMatch() {
            registerRequest.setConfirmPassword("different_password");

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BusinessException.class);

            verify(userRepository, never()).existsByUsername(any());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameAlreadyExists() {
            when(userRepository.existsByUsername(username)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(ResourceExistsException.class);

            verify(userRepository).existsByUsername(username);
            verify(userRepository, never()).existsByEmail(any());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(userRepository.existsByEmail(email)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(ResourceExistsException.class);

            verify(userRepository).existsByUsername(username);
            verify(userRepository).existsByEmail(email);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when role not found")
        void shouldThrowExceptionWhenRoleNotFound() {
            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
            when(roleRepository.findByName(RoleType.USER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(roleRepository).findByName(RoleType.USER);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should encode password before saving")
        void shouldEncodePasswordBeforeSaving() {
            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
            when(roleRepository.findByName(RoleType.USER)).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(jwtProvider.generateAccessToken(any(User.class))).thenReturn(accessToken);
            when(jwtProvider.generateRefreshToken()).thenReturn(refreshToken);
            when(jwtProvider.getAccessTokenExpiration()).thenReturn(expiresIn);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
            when(userMapper.toDto(any(User.class))).thenReturn(userDto);

            authService.register(registerRequest);

            verify(passwordEncoder).encode(password);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo(encodedPassword);
        }

        @Test
        @DisplayName("Should assign USER role to new user")
        void shouldAssignUserRoleToNewUser() {
            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
            when(roleRepository.findByName(RoleType.USER)).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(jwtProvider.generateAccessToken(any(User.class))).thenReturn(accessToken);
            when(jwtProvider.generateRefreshToken()).thenReturn(refreshToken);
            when(jwtProvider.getAccessTokenExpiration()).thenReturn(expiresIn);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
            when(userMapper.toDto(any(User.class))).thenReturn(userDto);

            authService.register(registerRequest);

            verify(roleRepository).findByName(RoleType.USER);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getRoles()).contains(role);
        }

    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("Should login successfully")
        void shouldLoginSuccessfully() {
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
            when(jwtProvider.generateAccessToken(user)).thenReturn(accessToken);
            when(jwtProvider.generateRefreshToken()).thenReturn(refreshToken);
            when(jwtProvider.getAccessTokenExpiration()).thenReturn(expiresIn);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
            when(userMapper.toDto(user)).thenReturn(userDto);

            AuthResponse result = authService.login(loginRequest);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo(accessToken);
            assertThat(result.getRefreshToken()).isEqualTo(refreshToken);
            assertThat(result.getExpiresIn()).isEqualTo(expiresIn);
            assertThat(result.getUser()).isEqualTo(userDto);

            verify(userRepository).findByUsername(username);
            verify(passwordEncoder).matches(password, encodedPassword);
            verify(jwtProvider).generateAccessToken(user);
            verify(jwtProvider).generateRefreshToken();
            verify(refreshTokenRepository).save(any(RefreshToken.class));
            verify(userMapper).toDto(user);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(AuthException.class);

            verify(userRepository).findByUsername(username);
            verify(passwordEncoder, never()).matches(any(), any());
            verify(jwtProvider, never()).generateAccessToken(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when password invalid")
        void shouldThrowExceptionWhenPasswordInvalid() {
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(password, encodedPassword)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(AuthException.class);

            verify(userRepository).findByUsername(username);
            verify(passwordEncoder).matches(password, encodedPassword);
            verify(jwtProvider, never()).generateAccessToken(any(User.class));
        }

        @Test
        @DisplayName("Should return generic error message when user not found")
        void shouldReturnGenericErrorMessageWhenUserNotFound() {
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(AuthException.class)
                    .extracting(Throwable::getMessage)
                    .isNotNull();
        }

        @Test
        @DisplayName("Should return same generic error message when password invalid")
        void shouldReturnSameGenericErrorMessageWhenPasswordInvalid() {
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(password, encodedPassword)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(AuthException.class)
                    .extracting(Throwable::getMessage)
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("Should refresh tokens successfully")
        void shouldRefreshTokensSuccessfully() {

            String newAccessToken = "new_access_token";
            String newRefreshToken = "new_refresh_token";

            when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(storedToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtProvider.generateAccessToken(user)).thenReturn(newAccessToken);
            when(jwtProvider.generateRefreshToken()).thenReturn(newRefreshToken);
            when(jwtProvider.getAccessTokenExpiration()).thenReturn(expiresIn);
            when(userMapper.toDto(user)).thenReturn(userDto);

            RefreshRequest request = RefreshRequest.builder().refreshToken(refreshToken).build();
            AuthResponse result = authService.refresh(request);

            assertThat(result.getAccessToken()).isEqualTo(newAccessToken);
            assertThat(result.getRefreshToken()).isEqualTo(newRefreshToken);
            assertThat(result.getExpiresIn()).isEqualTo(expiresIn);
            assertThat(result.getUser()).isEqualTo(userDto);
            assertThat(storedToken.getRevoked()).isTrue();

            verify(refreshTokenRepository).findByToken(refreshToken);
        }

        @Test
        @DisplayName("Should throw exception when refresh token not found")
        void shouldThrowExceptionWhenRefreshTokenNotFound() {
            when(refreshTokenRepository.findByToken("invalid_token")).thenReturn(Optional.empty());

            RefreshRequest request = RefreshRequest.builder().refreshToken("invalid_token").build();

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("Should throw exception when refresh token is revoked")
        void shouldThrowExceptionWhenRefreshTokenIsRevoked() {
            RefreshToken revokedToken = RefreshToken.builder()
                    .id(1L)
                    .token(refreshToken)
                    .user(user)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revoked(true)
                    .build();

            when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(revokedToken));

            RefreshRequest request = RefreshRequest.builder().refreshToken(refreshToken).build();

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("Should throw exception when refresh token is expired")
        void shouldThrowExceptionWhenRefreshTokenIsExpired() {
            RefreshToken expiredToken = RefreshToken.builder()
                    .id(1L)
                    .token(refreshToken)
                    .user(user)
                    .expiresAt(Instant.now().minusSeconds(3600))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(expiredToken));

            RefreshRequest request = RefreshRequest.builder().refreshToken(refreshToken).build();

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(AuthException.class);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("Should revoke refresh token on logout")
        void shouldRevokeRefreshTokenOnLogout() {
            when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(storedToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            authService.logout(refreshToken);

            assertThat(storedToken.getRevoked()).isTrue();
            verify(refreshTokenRepository).save(storedToken);
        }

        @Test
        @DisplayName("Should do nothing when refresh token not found on logout")
        void shouldDoNothingWhenRefreshTokenNotFoundOnLogout() {
            when(refreshTokenRepository.findByToken("unknown_token")).thenReturn(Optional.empty());

            authService.logout("unknown_token");

            verify(refreshTokenRepository, never()).save(any());
        }
    }
}
