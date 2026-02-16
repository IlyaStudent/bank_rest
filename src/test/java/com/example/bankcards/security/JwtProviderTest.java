package com.example.bankcards.security;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String TEST_SECRET =
            "dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciB0ZXN0aW5nIEhTNTEyIHNpZ25hdHVyZSBhbGdvcml0aG0gMTIzNDU2Nzg5MA==";
    private static final long ACCESS_EXPIRATION = 3600000L;

    private JwtProvider jwtProvider;
    private User user;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(TEST_SECRET, ACCESS_EXPIRATION);

        Role role = Role.builder()
                .id(1L)
                .name(RoleType.USER)
                .build();

        user = User.builder()
                .id(1L)
                .username("test")
                .email("test@example.com")
                .password("password")
                .roles(new HashSet<>(Set.of(role)))
                .build();
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("Should contain correct subject (userId)")
        void shouldContainCorrectSubject() {
            String token = jwtProvider.generateAccessToken(user);

            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
            String subject = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().getSubject();

            assertThat(subject).isEqualTo("1");
        }

        @Test
        @DisplayName("Should contain username claim")
        void shouldContainUsernameClaim() {
            String token = jwtProvider.generateAccessToken(user);

            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
            String username = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().get("username", String.class);

            assertThat(username).isEqualTo("test");
        }

        @Test
        @DisplayName("Should contain roles claim")
        void shouldContainRolesClaim() {
            String token = jwtProvider.generateAccessToken(user);

            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
            @SuppressWarnings("unchecked")
            List<String> roles = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().get("roles", List.class);

            assertThat(roles).containsExactly("USER");
        }

        @Test
        @DisplayName("Should contain jti as UUID")
        void shouldContainJtiAsUuid() {
            String token = jwtProvider.generateAccessToken(user);

            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
            String jti = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().getId();

            assertThat(jti).isNotNull();
            assertThat(UUID.fromString(jti)).isNotNull();
        }

        @Test
        @DisplayName("Should have expiration in the future")
        void shouldHaveExpirationInFuture() {
            String token = jwtProvider.generateAccessToken(user);

            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
            Date expiration = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().getExpiration();

            assertThat(expiration).isAfter(new Date());
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshToken {

        @Test
        @DisplayName("Should return UUID string")
        void shouldReturnUuidString() {
            String refreshToken = jwtProvider.generateRefreshToken();

            assertThat(refreshToken).isNotNull();
            assertThat(UUID.fromString(refreshToken)).isNotNull();
        }

        @Test
        @DisplayName("Should return unique tokens on each call")
        void shouldReturnUniqueTokens() {
            String token1 = jwtProvider.generateRefreshToken();
            String token2 = jwtProvider.generateRefreshToken();

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("Should return true for valid token")
        void shouldReturnTrueForValidToken() {
            String token = jwtProvider.generateAccessToken(user);

            assertThat(jwtProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should return false for expired token")
        void shouldReturnFalseForExpiredToken() {
            JwtProvider expiredProvider = new JwtProvider(TEST_SECRET, -1000L);
            String token = expiredProvider.generateAccessToken(user);

            assertThat(jwtProvider.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("Should return false for token signed with different key")
        void shouldReturnFalseForDifferentKey() {
            String otherSecret =
                    "YW5vdGhlciB2ZXJ5IGxvbmcgc2VjcmV0IGtleSBmb3IgdGVzdGluZyBIUzUxMiBzaWduYXR1cmUgYWxnb3JpdGhtIDk4NzY1NDMyMTA=";
            JwtProvider otherProvider = new JwtProvider(otherSecret, ACCESS_EXPIRATION);
            String token = otherProvider.generateAccessToken(user);

            assertThat(jwtProvider.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("Should return false for malformed string")
        void shouldReturnFalseForMalformedString() {
            assertThat(jwtProvider.validateToken("not.a.jwt")).isFalse();
        }
    }

    @Nested
    @DisplayName("getUsername")
    class GetUsername {

        @Test
        @DisplayName("Should extract username from valid token")
        void shouldExtractUsername() {
            String token = jwtProvider.generateAccessToken(user);

            assertThat(jwtProvider.getUsername(token)).isEqualTo("test");
        }
    }

    @Nested
    @DisplayName("getJti")
    class GetJti {

        @Test
        @DisplayName("Should extract jti from valid token")
        void shouldExtractJti() {
            String token = jwtProvider.generateAccessToken(user);

            String jti = jwtProvider.getJti(token);

            assertThat(jti).isNotNull();
            assertThat(UUID.fromString(jti)).isNotNull();
        }
    }

    @Nested
    @DisplayName("getRemainingTtlMillis")
    class GetRemainingTtlMillis {

        @Test
        @DisplayName("Should return positive value for valid token")
        void shouldReturnPositiveForValidToken() {
            String token = jwtProvider.generateAccessToken(user);

            assertThat(jwtProvider.getRemainingTtlMillis(token)).isGreaterThan(0L);
        }

        @Test
        @DisplayName("Should return zero for expired token")
        void shouldReturnZeroForExpiredToken() {
            JwtProvider expiredProvider = new JwtProvider(TEST_SECRET, -1000L);
            String token = expiredProvider.generateAccessToken(user);

            assertThat(jwtProvider.getRemainingTtlMillis(token)).isZero();
        }
    }

    @Nested
    @DisplayName("getAccessTokenExpiration")
    class GetAccessTokenExpiration {

        @Test
        @DisplayName("Should return instant in the future")
        void shouldReturnInstantInFuture() {
            Instant expiration = jwtProvider.getAccessTokenExpiration();

            assertThat(expiration).isAfter(Instant.now());
        }
    }
}
