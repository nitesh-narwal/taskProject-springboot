package com.example.shopapi.repository.mongo;

import com.example.shopapi.document.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, Order.OrderStatus status, Pageable pageable);

    Optional<Order> findByPaymentSessionId(String paymentSessionId);

    Optional<Order> findByPaymentId(String paymentId);

    @Query("{'createdAt': {$gte: ?0, $lte: ?1}}")
    Page<Order> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("{'userId': ?0, 'createdAt': {$gte: ?1, $lte: ?2}}")
    Page<Order> findByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    long countByStatus(Order.OrderStatus status);

    long countByUserId(Long userId);

    @Query(value = "{'createdAt': {$gte: ?0}}", count = true)
    long countOrdersSince(LocalDateTime since);

    @Query("{'status': {$in: ['PROCESSING', 'SHIPPED']}}")
    Page<Order> findActiveOrders(Pageable pageable);

    @Query("{'status': 'PAID', 'createdAt': {$gte: ?0}}")
    List<Order> findPaidOrdersSince(LocalDateTime since);

    @Query(value = "{'status': ?0, 'createdAt': {$gte: ?1}}", count = true)
    long countByStatusSince(Order.OrderStatus status, LocalDateTime since);
}

