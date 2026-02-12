package com.example.shopapi.service;

import com.example.shopapi.document.Cart;
import com.example.shopapi.document.Order;
import com.example.shopapi.document.Product;
import com.example.shopapi.dto.order.CreateOrderRequest;
import com.example.shopapi.dto.order.OrderResponse;
import com.example.shopapi.dto.order.UpdateOrderStatusRequest;
import com.example.shopapi.entity.User;
import com.example.shopapi.exception.InsufficientStockException;
import com.example.shopapi.exception.ResourceNotFoundException;
import com.example.shopapi.repository.jpa.UserRepository;
import com.example.shopapi.repository.mongo.CartMongoRepository;
import com.example.shopapi.repository.mongo.OrderRepository;
import com.example.shopapi.repository.mongo.ProductRepository;
import com.example.shopapi.security.UserPrincipal;
import com.example.shopapi.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartMongoRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Long userId = getCurrentUserId();
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // Validate stock and create order items
        List<Order.OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", cartItem.getProductId()));

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(product.getId(), cartItem.getQuantity(), product.getStockQuantity());
            }

            // Reduce stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            return Order.OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .price(cartItem.getPrice())
                    .quantity(cartItem.getQuantity())
                    .imageUrl(product.getImageUrl())
                    .build();
        }).collect(Collectors.toList());

        Order order = Order.builder()
                .userId(userId)
                .items(orderItems)
                .status(Order.OrderStatus.CREATED)
                .shippingAddress(mapShippingAddress(request.getShippingAddress()))
                .billingAddress(request.getBillingAddress() != null ? mapBillingAddress(request.getBillingAddress()) : null)
                .notes(request.getNotes())
                .build();

        order.calculateTotals();
        order = orderRepository.save(order);

        // Clear cart
        cart.clear();
        cartRepository.save(cart);

        auditService.logAction("ORDER_CREATED", "ORDER", order.getId(), "Order created with " + orderItems.size() + " items");
        log.info("Order created: {} for user {}", order.getId(), userId);

        return mapToResponse(order);
    }

    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();

        // Users can only view their own orders
        if ("ROLE_USER".equals(role) && !order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order", "id", orderId);
        }

        return mapToResponse(order);
    }

    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        Long userId = getCurrentUserId();
        return orderRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }

    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::mapToResponse);
    }

    public Page<OrderResponse> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable).map(this::mapToResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(request.getStatus());

        if (request.getTrackingNumber() != null) {
            order.setTrackingNumber(request.getTrackingNumber());
        }

        switch (request.getStatus()) {
            case SHIPPED -> {
                order.setShippedAt(LocalDateTime.now());
                sendOrderShippedEmail(order);
            }
            case DELIVERED -> order.setDeliveredAt(LocalDateTime.now());
            case CANCELLED -> {
                order.setCancelledAt(LocalDateTime.now());
                order.setCancellationReason(request.getNotes());
                restoreStock(order);
            }
            default -> {}
        }

        order = orderRepository.save(order);
        auditService.logAction("ORDER_STATUS_UPDATED", "ORDER", order.getId(),
                "Status changed from " + oldStatus + " to " + request.getStatus());
        log.info("Order {} status updated to {}", orderId, request.getStatus());

        return mapToResponse(order);
    }

    public void markOrderAsPaid(String orderId, String paymentId, String paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        order.setStatus(Order.OrderStatus.PAID);
        order.setPaymentId(paymentId);
        order.setPaymentMethod(paymentMethod);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        // Send confirmation email
        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            emailService.sendOrderConfirmationEmail(user.getEmail(), user.getFullName(), order.getId(), order.getTotalAmount().toString());
        }

        auditService.logAction("ORDER_PAID", "ORDER", order.getId(), "Payment received: " + paymentId);
    }

    public void markOrderAsFailed(String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        order.setStatus(Order.OrderStatus.FAILED);
        orderRepository.save(order);

        restoreStock(order);

        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            emailService.sendPaymentFailedEmail(user.getEmail(), user.getFullName(), order.getId(), reason);
        }

        auditService.logAction("ORDER_FAILED", "ORDER", order.getId(), "Payment failed: " + reason);
    }

    private void restoreStock(Order order) {
        for (Order.OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }
    }

    private void sendOrderShippedEmail(Order order) {
        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            emailService.sendOrderShippedEmail(user.getEmail(), user.getFullName(), order.getId(), order.getTrackingNumber());
        }
    }

    private Order.ShippingAddress mapShippingAddress(CreateOrderRequest.ShippingAddressDto dto) {
        return Order.ShippingAddress.builder()
                .fullName(dto.getFullName()).addressLine1(dto.getAddressLine1())
                .addressLine2(dto.getAddressLine2()).city(dto.getCity())
                .state(dto.getState()).postalCode(dto.getPostalCode())
                .country(dto.getCountry()).phone(dto.getPhone()).build();
    }

    private Order.BillingAddress mapBillingAddress(CreateOrderRequest.BillingAddressDto dto) {
        return Order.BillingAddress.builder()
                .fullName(dto.getFullName()).addressLine1(dto.getAddressLine1())
                .addressLine2(dto.getAddressLine2()).city(dto.getCity())
                .state(dto.getState()).postalCode(dto.getPostalCode())
                .country(dto.getCountry()).build();
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId()).userId(order.getUserId())
                .items(order.getItems().stream().map(i -> OrderResponse.OrderItemDto.builder()
                        .productId(i.getProductId()).productName(i.getProductName())
                        .productSku(i.getProductSku()).price(i.getPrice())
                        .quantity(i.getQuantity()).imageUrl(i.getImageUrl())
                        .subtotal(i.getSubtotal()).build()).collect(Collectors.toList()))
                .subtotal(order.getSubtotal()).tax(order.getTax())
                .shippingCost(order.getShippingCost()).discount(order.getDiscount())
                .totalAmount(order.getTotalAmount()).status(order.getStatus())
                .paymentId(order.getPaymentId()).paymentMethod(order.getPaymentMethod())
                .trackingNumber(order.getTrackingNumber()).notes(order.getNotes())
                .totalItems(order.getTotalItems()).createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt()).paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt()).deliveredAt(order.getDeliveredAt()).build();
    }

    private Long getCurrentUserId() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }

    private String getCurrentUserRole() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getRole();
    }
}

