package com.example.bankcards.security;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleType;
import com.example.bankcards.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtProvider {
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_USERNAME = "username";

    private final SecretKey secretKey;
    private final long accessExpiration;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiration}") long accessExpiration
    ) {
        this.secretKey = getKey(secret);
        this.accessExpiration = accessExpiration;
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_ROLES, getRoles(user))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return !claims.getPayload().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return getAllClaimsFromToken(token).get(CLAIM_USERNAME, String.class);
    }

    @NonNull
    public Instant getAccessTokenExpiration() {
        return Instant.now().plus(Duration.ofMillis(accessExpiration));
    }

    private List<String> getRoles(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .map(RoleType::name)
                .collect(Collectors.toList());
    }

    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    private SecretKey getKey(String secretKey64) {
        byte[] decode64 = Decoders.BASE64.decode(secretKey64);
        return Keys.hmacShaKeyFor(decode64);
    }
}
