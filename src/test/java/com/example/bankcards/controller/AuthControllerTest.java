package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.config.TestSecurityConfig;
import com.example.bankcards.dto.authentication.AuthResponse;
import com.example.bankcards.dto.authentication.LoginRequest;
import com.example.bankcards.dto.authentication.RegisterRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.exception.AuthException;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ResourceExistsException;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;
    private String username;
    private String email;
    private String password;
    private String token;

    private static final Instant FIXED_TIMESTAMP = Instant.parse("2026-01-01T12:00:00Z");

    private static final String AUTH_URL = "/api/auth";
    private static final String REGISTER_URL = AUTH_URL + "/register";
    private static final String LOGIN_URL = AUTH_URL + "/login";

    @BeforeEach
    void setUp() {
        username = "test";
        email = "test@example.com";
        password = "password";
        token = "jwt.token.here";

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

        UserDto userDto = UserDto.builder()
                .id(1L)
                .username(username)
                .email(email)
                .build();

        authResponse = AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .expiresIn(FIXED_TIMESTAMP)
                .user(userDto)
                .build();
    }

    @Nested
    @DisplayName("POST " + REGISTER_URL)
    class Register {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUserSuccessfully() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value(token))
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.user.username").value(username));

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when validation fails")
        void shouldReturn400WhenValidationFails() throws Exception {
            RegisterRequest invalidRequest = RegisterRequest.builder()
                    .username("")
                    .email("invalid-email")
                    .password("123")
                    .confirmPassword("")
                    .build();

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Should return 409 when username already exists")
        void shouldReturn409WhenUsernameExists() throws Exception {
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(ResourceExistsException.username(username));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isConflict());

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Should return 409 when email already exists")
        void shouldReturn409WhenEmailExists() throws Exception {
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(ResourceExistsException.email(email));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest)))
                    .andExpect(status().isConflict());

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Should return 422 when passwords do not match")
        void shouldReturn422WhenPasswordsDoNotMatch() throws Exception {
            RegisterRequest mismatchRequest = RegisterRequest.builder()
                    .username(username)
                    .email(email)
                    .password(password)
                    .confirmPassword("different_password")
                    .build();
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(BusinessException.passwordsDoNotMatch());

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(mismatchRequest)))
                    .andExpect(status().isUnprocessableEntity());

            verify(authService).register(any(RegisterRequest.class));
        }
    }

    @Nested
    @DisplayName("POST " + LOGIN_URL)
    class Login {

        @Test
        @DisplayName("Should login successfully")
        void shouldLoginSuccessfully() throws Exception {
            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(token))
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.user.username").value(username));

            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when validation fails")
        void shouldReturn400WhenValidationFails() throws Exception {
            LoginRequest invalidRequest = LoginRequest.builder()
                    .username("")
                    .password("")
                    .build();

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("Should return 401 when credentials are invalid")
        void shouldReturn401WhenCredentialsInvalid() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(AuthException.invalidCredentials());

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());

            verify(authService).login(any(LoginRequest.class));
        }
    }
}
