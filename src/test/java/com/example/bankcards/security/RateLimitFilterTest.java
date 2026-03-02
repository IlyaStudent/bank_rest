package com.example.bankcards.security;

import com.example.bankcards.config.RateLimitProperties;
import com.example.bankcards.util.constants.ApiErrorMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LettuceBasedProxyManager<String> proxyManager;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private FilterChain filterChain;

    @Mock
    private BucketProxy bucketProxy;

    @Mock
    private ConsumptionProbe consumptionProbe;

    private RateLimitFilter rateLimitFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String VALID_TOKEN = "valid.jwt.token";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        RateLimitProperties rateLimitProperties = createRateLimitProperties();
        rateLimitFilter = new RateLimitFilter(
                proxyManager, rateLimitProperties, jwtProvider, objectMapper
        );
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    private RateLimitProperties createRateLimitProperties() {
        RateLimitProperties props = new RateLimitProperties();

        RateLimitProperties.LimitRule login = new RateLimitProperties.LimitRule();
        login.setCapacity(5);
        login.setMinutes(1);
        props.setLogin(login);

        RateLimitProperties.LimitRule register = new RateLimitProperties.LimitRule();
        register.setCapacity(3);
        register.setMinutes(1);
        props.setRegister(register);

        RateLimitProperties.LimitRule transfers = new RateLimitProperties.LimitRule();
        transfers.setCapacity(10);
        transfers.setMinutes(1);
        props.setTransfers(transfers);

        RateLimitProperties.LimitRule general = new RateLimitProperties.LimitRule();
        general.setCapacity(100);
        general.setMinutes(1);
        props.setGeneral(general);

        return props;
    }

    private void setupBucketConsumed(long remaining) {
        when(proxyManager.builder().build(anyString(), ArgumentMatchers.<Supplier<BucketConfiguration>>any())).thenReturn(bucketProxy);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
        when(consumptionProbe.isConsumed()).thenReturn(true);
        when(consumptionProbe.getRemainingTokens()).thenReturn(remaining);
    }

    private void setupBucketRejected(long nanosToRefill) {
        when(proxyManager.builder().build(anyString(), ArgumentMatchers.<Supplier<BucketConfiguration>>any())).thenReturn(bucketProxy);
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
        when(consumptionProbe.isConsumed()).thenReturn(false);
        when(consumptionProbe.getNanosToWaitForRefill()).thenReturn(nanosToRefill);
    }

    @Nested
    @DisplayName("Skip paths")
    class SkipPaths {

        @ParameterizedTest(name = "Should pass through for path: {0}")
        @ValueSource(strings = {"/swagger-ui/index.html", "/actuator/health", "/v3/api-docs", "/some/other/path"})
        void shouldPassThroughForSkippedPaths(String uri) throws ServletException, IOException {
            request.setRequestURI(uri);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(bucketProxy, never()).tryConsumeAndReturnRemaining(anyLong());
        }
    }

    @Nested
    @DisplayName("Request within limit")
    class RequestWithinLimit {

        @Test
        @DisplayName("Should allow login when within limit and add remaining header")
        void shouldAllowLoginWhenWithinLimit() throws ServletException, IOException {
            request.setRequestURI("/api/auth/login");
            request.setMethod("POST");
            setupBucketConsumed(4);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-Rate-Limit-Remaining")).isEqualTo("4");
        }

        @Test
        @DisplayName("Should allow transfer with valid token")
        void shouldAllowTransferWithValidToken() throws ServletException, IOException {
            request.setRequestURI("/api/transfers");
            request.setMethod("POST");
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            when(jwtProvider.getUsername(VALID_TOKEN)).thenReturn("testuser");
            setupBucketConsumed(9);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-Rate-Limit-Remaining")).isEqualTo("9");
        }

        @Test
        @DisplayName("Should apply general limit for GET API paths")
        void shouldApplyGeneralLimitForGetApiPaths() throws ServletException, IOException {
            request.setRequestURI("/api/cards");
            request.setMethod("GET");
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            when(jwtProvider.getUsername(VALID_TOKEN)).thenReturn("testuser");
            setupBucketConsumed(99);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-Rate-Limit-Remaining")).isEqualTo("99");
        }
    }

    @Nested
    @DisplayName("Rate limit exceeded")
    class RateLimitExceeded {

        @Test
        @DisplayName("Should return 429 with correct body when login limit exceeded")
        void shouldReturn429WhenLoginLimitExceeded() throws ServletException, IOException {
            request.setRequestURI("/api/auth/login");
            request.setMethod("POST");
            setupBucketRejected(60_000_000_000L);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
            assertThat(response.getHeader("Retry-After")).isEqualTo("61");

            @SuppressWarnings("unchecked")
            Map<String, Object> body = new ObjectMapper().readValue(
                    response.getContentAsString(), Map.class);
            assertThat(body)
                    .containsEntry("status", 429)
                    .containsEntry("error", "Too Many Requests")
                    .containsEntry("message", ApiErrorMessage.RATE_LIMIT_EXCEEDED.getMessage())
                    .containsEntry("path", "/api/auth/login");
        }

        @Test
        @DisplayName("Should return 429 when register limit exceeded")
        void shouldReturn429WhenRegisterLimitExceeded() throws ServletException, IOException {
            request.setRequestURI("/api/auth/register");
            request.setMethod("POST");
            setupBucketRejected(45_000_000_000L);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            assertThat(response.getHeader("Retry-After")).isEqualTo("46");
        }

        @Test
        @DisplayName("Should return 429 when transfer limit exceeded")
        void shouldReturn429WhenTransferLimitExceeded() throws ServletException, IOException {
            request.setRequestURI("/api/transfers");
            request.setMethod("POST");
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            when(jwtProvider.getUsername(VALID_TOKEN)).thenReturn("testuser");
            setupBucketRejected(30_000_000_000L);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        }
    }

    @Nested
    @DisplayName("No bucket key")
    class NoBucketKey {

        @Test
        @DisplayName("Should pass through when no token for protected endpoint")
        void shouldPassThroughWhenNoTokenForProtectedEndpoint() throws ServletException, IOException {
            request.setRequestURI("/api/transfers");
            request.setMethod("POST");

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(bucketProxy, never()).tryConsumeAndReturnRemaining(anyLong());
        }

        @Test
        @DisplayName("Should pass through when token is invalid")
        void shouldPassThroughWhenTokenIsInvalid() throws ServletException, IOException {
            request.setRequestURI("/api/cards");
            request.setMethod("GET");
            request.addHeader("Authorization", "Bearer invalid.token");
            when(jwtProvider.getUsername("invalid.token"))
                    .thenThrow(new RuntimeException("Invalid"));

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(bucketProxy, never()).tryConsumeAndReturnRemaining(anyLong());
        }
    }

    @Nested
    @DisplayName("Client IP resolution")
    class ClientIpResolution {

        @Test
        @DisplayName("Should use X-Forwarded-For when present")
        void shouldUseXForwardedForWhenPresent() throws ServletException, IOException {
            request.setRequestURI("/api/auth/login");
            request.setMethod("POST");
            request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18");
            request.setRemoteAddr("127.0.0.1");
            setupBucketConsumed(4);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should use remote address when no X-Forwarded-For")
        void shouldUseRemoteAddressWhenNoForwardedHeader() throws ServletException, IOException {
            request.setRequestURI("/api/auth/login");
            request.setMethod("POST");
            request.setRemoteAddr("192.168.1.100");
            setupBucketConsumed(4);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }
}
