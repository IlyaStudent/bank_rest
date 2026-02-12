package com.example.bankcards.security;

import com.example.bankcards.util.constants.ApiErrorMessage;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final CustomUserDetailService userDetailService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        String requestURI = request.getRequestURI();

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String jwt = authHeader.substring(BEARER_PREFIX.length());
            try {
                if (!jwtProvider.validateToken(jwt)) {
                    sendErrorResponse(response, HttpStatus.UNAUTHORIZED, ApiErrorMessage.TOKEN_EXPIRED.getMessage());
                    return;
                }

                String username = jwtProvider.getUsername(jwt);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    log.debug("User authenticated: username='{}', uri={}", username, requestURI);
                }

            } catch (ExpiredJwtException e) {
                log.warn("JWT token expired: uri={}", requestURI);
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, ApiErrorMessage.TOKEN_EXPIRED.getMessage());
                return;
            } catch (SignatureException | MalformedJwtException e) {
                log.warn("Invalid JWT signature: uri={}", requestURI);
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, ApiErrorMessage.INVALID_TOKEN_SIGNATURE.getMessage());
                return;
            } catch (Exception e) {
                log.error(ApiErrorMessage.ERROR_DURING_JWT_PROCESSING.getMessage(), e);
                sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorMessage.UNEXPECTED_ERROR.getMessage());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.getWriter().write(message);
    }
}
