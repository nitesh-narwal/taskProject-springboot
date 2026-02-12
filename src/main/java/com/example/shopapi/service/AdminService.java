package com.example.shopapi.service;

import com.example.shopapi.document.Order;
import com.example.shopapi.dto.admin.CreateWorkerRequest;
import com.example.shopapi.dto.admin.SystemStatsResponse;
import com.example.shopapi.dto.user.UserProfileResponse;
import com.example.shopapi.entity.PaymentTransaction;
import com.example.shopapi.entity.Role;
import com.example.shopapi.entity.User;
import com.example.shopapi.exception.ResourceNotFoundException;
import com.example.shopapi.exception.UserAlreadyExistsException;
import com.example.shopapi.repository.jpa.PaymentTransactionRepository;
import com.example.shopapi.repository.jpa.UserRepository;
import com.example.shopapi.repository.mongo.OrderRepository;
import com.example.shopapi.repository.mongo.ProductRepository;
import com.example.shopapi.repository.mongo.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public UserProfileResponse createWorker(CreateWorkerRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw UserAlreadyExistsException.forUsername(request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw UserAlreadyExistsException.forEmail(request.getEmail());
        }

        User worker = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(Role.ROLE_WORKER)
                .enabled(true)
                .emailVerified(true)
                .accountNonLocked(true)
                .build();

        worker = userRepository.save(worker);
        auditService.logAction("WORKER_CREATED", "USER", worker.getId().toString(), "Worker created: " + worker.getUsername());
        log.info("Worker created: {}", worker.getUsername());
        return mapToProfileResponse(worker);
    }

    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToProfileResponse);
    }

    public Page<UserProfileResponse> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role).stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())
                ));
    }

    public List<UserProfileResponse> getWorkers() {
        return userRepository.findByRole(Role.ROLE_WORKER).stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserProfileResponse toggleUserStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Cannot modify admin status");
        }

        user.setEnabled(enabled);
        user = userRepository.save(user);

        auditService.logAction(enabled ? "USER_ENABLED" : "USER_DISABLED", "USER", userId.toString(),
                "User " + user.getUsername() + " " + (enabled ? "enabled" : "disabled"));
        log.info("User {} status changed to {}", userId, enabled);
        return mapToProfileResponse(user);
    }

    public SystemStatsResponse getSystemStats() {
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        long totalUsers = userRepository.countByRole(Role.ROLE_USER);
        long totalWorkers = userRepository.countByRole(Role.ROLE_WORKER);
        long totalAdmins = userRepository.countByRole(Role.ROLE_ADMIN);
        long activeUsers = userRepository.countActiveUsers();
        long newUsersThisMonth = userRepository.countNewUsersAfter(monthStart);

        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countActiveProducts();
        long outOfStockProducts = productRepository.findLowStockProducts(1).size();

        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(Order.OrderStatus.CREATED)
                + orderRepository.countByStatus(Order.OrderStatus.PENDING_PAYMENT);
        long completedOrders = orderRepository.countByStatus(Order.OrderStatus.DELIVERED);
        long failedOrders = orderRepository.countByStatus(Order.OrderStatus.FAILED);

        long successfulPayments = paymentTransactionRepository.countByStatus(PaymentTransaction.PaymentStatus.COMPLETED);
        long failedPayments = paymentTransactionRepository.countByStatus(PaymentTransaction.PaymentStatus.FAILED);
        BigDecimal totalRevenue = paymentTransactionRepository.sumAmountByStatus(PaymentTransaction.PaymentStatus.COMPLETED);
        BigDecimal revenueThisMonth = paymentTransactionRepository.sumAmountByStatusSince(PaymentTransaction.PaymentStatus.COMPLETED, monthStart);

        long openTickets = supportTicketRepository.countOpenTickets();
        long resolvedTickets = supportTicketRepository.countByStatus(com.example.shopapi.document.SupportTicket.TicketStatus.RESOLVED);
        long totalTickets = supportTicketRepository.count();

        return SystemStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalWorkers(totalWorkers)
                .totalAdmins(totalAdmins)
                .activeUsers(activeUsers)
                .newUsersThisMonth(newUsersThisMonth)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .outOfStockProducts(outOfStockProducts)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .failedOrders(failedOrders)
                .successfulPayments(successfulPayments)
                .failedPayments(failedPayments)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .revenueThisMonth(revenueThisMonth != null ? revenueThisMonth : BigDecimal.ZERO)
                .openTickets(openTickets)
                .resolvedTickets(resolvedTickets)
                .totalTickets(totalTickets)
                .build();
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
}

