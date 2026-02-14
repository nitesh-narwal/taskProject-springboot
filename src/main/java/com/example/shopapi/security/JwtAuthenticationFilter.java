package com.example.shopapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            String requestURI = request.getRequestURI();
            String method = request.getMethod();

            if (StringUtils.hasText(jwt)) {
                log.debug("Processing JWT token for {} {}", method, requestURI);

                if (tokenProvider.validateToken(jwt)) {
                    if (tokenProvider.isAccessToken(jwt)) {
                        Long userId = tokenProvider.getUserIdFromToken(jwt);
                        String role = tokenProvider.getRoleFromToken(jwt);
                        log.debug("Valid access token for user ID: {}, role: {}", userId, role);

                        UserDetails userDetails = customUserDetailsService.loadUserById(userId);

                        if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.debug("Authentication set for user: {} with authorities: {}",
                                    userDetails.getUsername(), userDetails.getAuthorities());
                        } else {
                            log.warn("User account is disabled or locked: {}", userDetails.getUsername());
                        }
                    } else {
                        log.warn("Token is not an access token for {} {}", method, requestURI);
                    }
                } else {
                    log.warn("Invalid JWT token for {} {}", method, requestURI);
                }
            } else {
                log.debug("No JWT token found for {} {}", method, requestURI);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context for {}: {}",
                    request.getRequestURI(), ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken == null) {
            return null;
        }

        log.debug("Authorization header received: {}",
                bearerToken.length() > 50 ? bearerToken.substring(0, 50) + "..." : bearerToken);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);

            // Handle common frontend issues
            if (token.startsWith("\"") && token.endsWith("\"")) {
                log.warn("Token wrapped in quotes, removing them");
                token = token.substring(1, token.length() - 1);
            }

            if (token.equals("undefined") || token.equals("null") || token.isEmpty()) {
                log.warn("Invalid token value: {}", token);
                return null;
            }

            return token;
        }

        // Some clients might send just the token without "Bearer "
        if (StringUtils.hasText(bearerToken) && bearerToken.contains(".") && bearerToken.split("\\.").length == 3) {
            log.warn("Authorization header missing 'Bearer ' prefix, but looks like a JWT");
            return bearerToken;
        }

        return null;
    }
}

