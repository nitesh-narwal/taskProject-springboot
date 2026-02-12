package com.example.shopapi.service;

import com.example.shopapi.dto.user.ChangePasswordRequest;
import com.example.shopapi.dto.user.UpdateProfileRequest;
import com.example.shopapi.dto.user.UserProfileResponse;
import com.example.shopapi.entity.User;
import com.example.shopapi.exception.ResourceNotFoundException;
import com.example.shopapi.repository.jpa.RefreshTokenRepository;
import com.example.shopapi.repository.jpa.UserRepository;
import com.example.shopapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserProfileResponse getMyProfile() {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());

        user = userRepository.save(user);
        auditService.logAction("PROFILE_UPDATED", "USER", userId.toString(), "User updated profile");
        log.info("Profile updated for user: {}", userId);
        return mapToProfileResponse(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllUserTokens(user);

        auditService.logAction("PASSWORD_CHANGED", "USER", userId.toString(), "User changed password");
        log.info("Password changed for user: {}", userId);
    }

    @Transactional
    public void deleteMyAccount() {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Soft delete - disable the account
        user.setEnabled(false);
        user.setAccountNonLocked(false);
        userRepository.save(user);

        // Revoke all tokens
        refreshTokenRepository.revokeAllUserTokens(user);

        auditService.logAction("ACCOUNT_DELETED", "USER", userId.toString(), "User deleted their account");
        log.info("Account deleted for user: {}", userId);
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private Long getCurrentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getId();
    }
}

