package com.example.shopapi.repository.jpa;

import com.example.shopapi.entity.AuditLog;
import com.example.shopapi.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByPerformedBy(String performedBy, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByRole(Role role, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.timestamp BETWEEN :startDate AND :endDate ORDER BY al.timestamp DESC")
    Page<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate,
                                    Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.action = :action AND al.timestamp >= :since ORDER BY al.timestamp DESC")
    List<AuditLog> findRecentByAction(@Param("action") String action, @Param("since") LocalDateTime since);

    @Query("SELECT al FROM AuditLog al WHERE al.success = false ORDER BY al.timestamp DESC")
    Page<AuditLog> findFailedActions(Pageable pageable);

    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.action = :action AND al.timestamp >= :since")
    long countByActionSince(@Param("action") String action, @Param("since") LocalDateTime since);

    @Query("SELECT al.action, COUNT(al) FROM AuditLog al WHERE al.timestamp >= :since GROUP BY al.action")
    List<Object[]> getActionStatsSince(@Param("since") LocalDateTime since);
}

