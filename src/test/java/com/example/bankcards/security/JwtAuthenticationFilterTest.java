package com.example.bankcards.security;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.RedisTokenService;
import com.example.bankcards.util.constants.ApiErrorMessage;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CustomUserDetailService userDetailService;

    @Mock
    private RedisTokenService redisTokenService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String VALID_TOKEN = "valid.jwt.token";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("No auth header")
    class NoAuthHeader {

        @Test
        @DisplayName("Should continue filter chain when no Authorization header")
        void shouldContinueFilterChainWhenNoAuthorizationHeader() throws ServletException, IOException {
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Non-bearer header")
    class NonBearerHeader {

        @Test
        @DisplayName("Should continue filter chain when header is not Bearer")
        void shouldContinueFilterChainWhenNotBearer() throws ServletException, IOException {
            request.addHeader("Authorization", "Basic abc123");

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Valid token")
    class ValidToken {

        @Test
        @DisplayName("Should set authentication in SecurityContext for valid token")
        void shouldSetAuthenticationForValidToken() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

            Role role = Role.builder().id(1L).name(RoleType.USER).build();
            User user = User.builder()
                    .id(1L)
                    .username("testuser")
                    .password("password")
                    .roles(new HashSet<>(Set.of(role)))
                    .build();
            CustomUserDetails userDetails = new CustomUserDetails(user);

            when(jwtProvider.validateToken(VALID_TOKEN)).thenReturn(true);
            when(jwtProvider.getJti(VALID_TOKEN)).thenReturn("test-jti");
            when(redisTokenService.isAccessTokenBlackListed("test-jti")).thenReturn(false);
            when(jwtProvider.getUsername(VALID_TOKEN)).thenReturn("testuser");
            when(userDetailService.loadUserByUsername("testuser")).thenReturn(userDetails);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("Invalid token")
    class InvalidToken {

        @Test
        @DisplayName("Should return 401 when token is invalid")
        void shouldReturn401WhenTokenInvalid() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

            when(jwtProvider.validateToken(VALID_TOKEN)).thenReturn(false);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(response.getContentAsString()).isEqualTo(ApiErrorMessage.TOKEN_EXPIRED.getMessage());
        }
    }

    @Nested
    @DisplayName("Blacklisted token")
    class BlacklistedToken {

        @Test
        @DisplayName("Should return 401 when token is blacklisted")
        void shouldReturn401WhenTokenBlacklisted() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

            when(jwtProvider.validateToken(VALID_TOKEN)).thenReturn(true);
            when(jwtProvider.getJti(VALID_TOKEN)).thenReturn("test-jti");
            when(redisTokenService.isAccessTokenBlackListed("test-jti")).thenReturn(true);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(response.getContentAsString()).isEqualTo(ApiErrorMessage.TOKEN_EXPIRED.getMessage());
        }
    }

    @Nested
    @DisplayName("Exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("Should return 401 when ExpiredJwtException thrown")
        void shouldReturn401WhenExpiredJwtException() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

            when(jwtProvider.validateToken(VALID_TOKEN))
                    .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(response.getContentAsString()).isEqualTo(ApiErrorMessage.TOKEN_EXPIRED.getMessage());
        }

        @Test
        @DisplayName("Should return 401 with INVALID_TOKEN_SIGNATURE when SignatureException thrown")
        void shouldReturn401WhenSignatureException() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

            when(jwtProvider.validateToken(VALID_TOKEN))
                    .thenThrow(new SignatureException("Invalid signature"));

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(response.getContentAsString()).isEqualTo(ApiErrorMessage.INVALID_TOKEN_SIGNATURE.getMessage());
        }

        @Test
        @DisplayName("Should return 401 with INVALID_TOKEN_SIGNATURE when MalformedJwtException thrown")
        void shouldReturn401WhenMalformedJwtException() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

            when(jwtProvider.validateToken(VALID_TOKEN))
                    .thenThrow(new MalformedJwtException("Malformed JWT"));

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(response.getContentAsString()).isEqualTo(ApiErrorMessage.INVALID_TOKEN_SIGNATURE.getMessage());
        }

        @Test
        @DisplayName("Should return 500 when unexpected exception thrown")
        void shouldReturn500WhenUnexpectedException() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);

            when(jwtProvider.validateToken(VALID_TOKEN))
                    .thenThrow(new RuntimeException("Something unexpected"));

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(response.getContentAsString()).isEqualTo(ApiErrorMessage.UNEXPECTED_ERROR.getMessage());
        }
    }
}
