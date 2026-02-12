package com.example.shopapi.repository.jpa;

import com.example.shopapi.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByOrderId(String orderId);

    Optional<PaymentTransaction> findByGatewayPaymentId(String gatewayPaymentId);

    Optional<PaymentTransaction> findByGatewaySessionId(String gatewaySessionId);

    List<PaymentTransaction> findByUserId(Long userId);

    Page<PaymentTransaction> findByUserId(Long userId, Pageable pageable);

    Page<PaymentTransaction> findByStatus(PaymentTransaction.PaymentStatus status, Pageable pageable);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.createdAt BETWEEN :startDate AND :endDate ORDER BY pt.createdAt DESC")
    Page<PaymentTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              Pageable pageable);

    @Query("SELECT COUNT(pt) FROM PaymentTransaction pt WHERE pt.status = :status")
    long countByStatus(@Param("status") PaymentTransaction.PaymentStatus status);

    @Query("SELECT SUM(pt.amount) FROM PaymentTransaction pt WHERE pt.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentTransaction.PaymentStatus status);

    @Query("SELECT SUM(pt.amount) FROM PaymentTransaction pt WHERE pt.status = :status AND pt.createdAt >= :since")
    BigDecimal sumAmountByStatusSince(@Param("status") PaymentTransaction.PaymentStatus status,
                                       @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(pt) FROM PaymentTransaction pt WHERE pt.createdAt >= :since")
    long countTransactionsSince(@Param("since") LocalDateTime since);

    @Query("SELECT pt.status, COUNT(pt), SUM(pt.amount) FROM PaymentTransaction pt WHERE pt.createdAt >= :since GROUP BY pt.status")
    List<Object[]> getPaymentStatsSince(@Param("since") LocalDateTime since);
}

