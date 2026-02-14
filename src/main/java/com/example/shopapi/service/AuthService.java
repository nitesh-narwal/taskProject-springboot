package com.example.shopapi.service;

import com.example.shopapi.dto.auth.*;
import com.example.shopapi.entity.*;
import com.example.shopapi.exception.*;
import com.example.shopapi.repository.jpa.*;
import com.example.shopapi.security.JwtTokenProvider;
import com.example.shopapi.security.UserPrincipal;
import com.example.shopapi.util.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${app.verification-token-expiry}")
    private long verificationTokenExpiry;

    @Value("${app.password-reset-token-expiry}")
    private long passwordResetTokenExpiry;

    @Transactional
    public String register(RegisterRequest request) {
        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw UserAlreadyExistsException.forUsername(request.getUsername());
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw UserAlreadyExistsException.forEmail(request.getEmail());
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(Role.ROLE_USER)
                .enabled(false)
                .emailVerified(false)
                .accountNonLocked(true)
                .build();

        user = userRepository.save(user);

        // Create verification token
        EmailVerificationToken verificationToken = EmailVerificationToken.create(
                user, verificationTokenExpiry / 60000); // Convert ms to minutes
        verificationTokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getFullName(),
                verificationToken.getToken()
        );

        auditService.logAction("USER_REGISTERED", "USER", user.getId().toString(),
                "New user registered: " + user.getUsername());

        log.info("User registered successfully: {}", user.getUsername());
        return "Registration successful. Please check your email to verify your account.";
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String ipAddress = getClientIpAddress();
        
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> {
                    auditService.logLoginAttempt(request.getUsernameOrEmail(), false, ipAddress, "User not found");
                    return new ResourceNotFoundException("User", "username/email", request.getUsernameOrEmail());
                });

        // Check if email is verified
        if (!user.getEmailVerified()) {
            auditService.logLoginAttempt(user.getUsername(), false, ipAddress, "Email not verified");
            throw new EmailNotVerifiedException();
        }

        // Check if account is enabled
        if (!user.getEnabled()) {
            auditService.logLoginAttempt(user.getUsername(), false, ipAddress, "Account disabled");
            throw new AccountDisabledException();
        }

        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getId());

        // Save refresh token
        saveRefreshToken(user, refreshToken);

        // Update last login
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now(), ipAddress);

        auditService.logLoginAttempt(user.getUsername(), true, ipAddress, "Login successful");

        log.info("User logged in successfully: {}", user.getUsername());

        return AuthResponse.of(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpiration(),
                AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().name())
                        .build()
        );
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        log.debug("Attempting to refresh token, token length: {}",
                requestRefreshToken != null ? requestRefreshToken.length() : "null");

        if (requestRefreshToken == null || requestRefreshToken.isEmpty()) {
            log.error("Refresh token is null or empty");
            throw InvalidTokenException.refreshToken();
        }

        // Check for common issues
        if (requestRefreshToken.startsWith("\"") || requestRefreshToken.equals("undefined") || requestRefreshToken.equals("null")) {
            log.error("Invalid refresh token format - received: {}",
                    requestRefreshToken.length() > 20 ? requestRefreshToken.substring(0, 20) + "..." : requestRefreshToken);
            throw InvalidTokenException.refreshToken();
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> {
                    log.error("Refresh token not found in database");
                    return InvalidTokenException.refreshToken();
                });

        if (!refreshToken.isValid()) {
            log.warn("Refresh token is invalid (revoked or expired)");
            refreshTokenRepository.revokeToken(requestRefreshToken);
            throw TokenExpiredException.refreshToken();
        }

        User user = refreshToken.getUser();

        // Generate new tokens
        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId());

        // Revoke old refresh token and save new one
        refreshTokenRepository.revokeToken(requestRefreshToken);
        saveRefreshToken(user, newRefreshToken);

        log.info("Token refreshed for user: {}", user.getUsername());

        return AuthResponse.of(
                newAccessToken,
                newRefreshToken,
                tokenProvider.getAccessTokenExpiration(),
                AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().name())
                        .build()
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.revokeToken(refreshToken);
        SecurityContextHolder.clearContext();
        log.info("User logged out");
    }

    @Transactional
    public void logoutAllDevices(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        refreshTokenRepository.revokeAllUserTokens(user);
        log.info("All devices logged out for user: {}", user.getUsername());
    }

    @Transactional
    public String verifyEmail(String token) {
        EmailVerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(InvalidTokenException::verificationToken);

        if (verificationToken.getUsed()) {
            throw new InvalidTokenException("Verification token has already been used");
        }

        if (verificationToken.isExpired()) {
            throw TokenExpiredException.verificationToken();
        }

        User user = verificationToken.getUser();
        userRepository.verifyEmail(user.getId());
        verificationTokenRepository.markAsUsed(token);

        auditService.logAction("EMAIL_VERIFIED", "USER", user.getId().toString(),
                "Email verified for: " + user.getEmail());

        log.info("Email verified for user: {}", user.getUsername());
        return "Email verified successfully. You can now login.";
    }

    @Transactional
    public String resendVerificationEmail(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (user.getEmailVerified()) {
            throw new IllegalStateException("Email is already verified");
        }

        // Delete existing tokens
        verificationTokenRepository.deleteByUser(user);

        // Create new token
        EmailVerificationToken verificationToken = EmailVerificationToken.create(
                user, verificationTokenExpiry / 60000);
        verificationTokenRepository.save(verificationToken);

        // Send email
        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getFullName(),
                verificationToken.getToken()
        );

        log.info("Verification email resent to: {}", user.getEmail());
        return "Verification email has been sent.";
    }

    @Transactional
    public String requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Delete existing reset tokens
        passwordResetTokenRepository.deleteByUser(user);

        // Create new token
        PasswordResetToken resetToken = PasswordResetToken.create(
                user, passwordResetTokenExpiry / 60000);
        passwordResetTokenRepository.save(resetToken);

        // Send email
        emailService.sendPasswordResetEmail(
                user.getEmail(),
                user.getFullName(),
                resetToken.getToken()
        );

        auditService.logAction("PASSWORD_RESET_REQUESTED", "USER", user.getId().toString(),
                "Password reset requested for: " + user.getEmail());

        log.info("Password reset email sent to: {}", user.getEmail());
        return "Password reset email has been sent.";
    }

    @Transactional
    public String resetPassword(PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(InvalidTokenException::passwordResetToken);

        if (resetToken.getUsed()) {
            throw new InvalidTokenException("Password reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw TokenExpiredException.passwordResetToken();
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.markAsUsed(request.getToken());

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllUserTokens(user);

        auditService.logAction("PASSWORD_RESET", "USER", user.getId().toString(),
                "Password reset completed for: " + user.getUsername());

        log.info("Password reset completed for user: {}", user.getUsername());
        return "Password has been reset successfully.";
    }

    private void saveRefreshToken(User user, String token) {
        log.debug("Saving refresh token for user: {}, token length: {}", user.getUsername(), token.length());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenExpiration() / 1000))
                .ipAddress(getClientIpAddress())
                .userAgent(getUserAgent())
                .build();
        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.debug("Refresh token saved with ID: {}", saved.getId());
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not get client IP address");
        }
        return "unknown";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("Could not get user agent");
        }
        return null;
    }
}

