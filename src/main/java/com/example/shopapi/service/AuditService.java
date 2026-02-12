package com.example.shopapi.service;

import com.example.shopapi.entity.AuditLog;
import com.example.shopapi.entity.Role;
import com.example.shopapi.repository.jpa.AuditLogRepository;
import com.example.shopapi.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void logAction(String action, String entityType, String entityId, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .performedBy(getCurrentUsername())
                    .role(getCurrentUserRole())
                    .ipAddress(getClientIpAddress())
                    .userAgent(getUserAgent())
                    .success(true)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} on {}:{}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    @Async
    @Transactional
    public void logAction(String action, String entityType, String entityId, String details,
                          String oldValue, String newValue) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .performedBy(getCurrentUsername())
                    .role(getCurrentUserRole())
                    .ipAddress(getClientIpAddress())
                    .userAgent(getUserAgent())
                    .success(true)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    @Async
    @Transactional
    public void logFailedAction(String action, String entityType, String entityId,
                                 String details, String errorMessage) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .performedBy(getCurrentUsername())
                    .role(getCurrentUserRole())
                    .ipAddress(getClientIpAddress())
                    .userAgent(getUserAgent())
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to create failed audit log: {}", e.getMessage());
        }
    }

    @Async
    @Transactional
    public void logLoginAttempt(String username, boolean success, String ipAddress, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(success ? "LOGIN_SUCCESS" : "LOGIN_FAILED")
                    .entityType("USER")
                    .entityId(username)
                    .details(details)
                    .performedBy(username)
                    .ipAddress(ipAddress)
                    .userAgent(getUserAgent())
                    .success(success)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to create login audit log: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUser(String username, Pageable pageable) {
        return auditLogRepository.findByPerformedBy(username, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByDateRange(startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getFailedActions(Pageable pageable) {
        return auditLogRepository.findFailedActions(pageable);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getUsername();
        }
        return "anonymous";
    }

    private Role getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            String role = ((UserPrincipal) authentication.getPrincipal()).getRole();
            try {
                return Role.valueOf(role);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
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
            log.debug("Could not get client IP address: {}", e.getMessage());
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
            log.debug("Could not get user agent: {}", e.getMessage());
        }
        return null;
    }
}

