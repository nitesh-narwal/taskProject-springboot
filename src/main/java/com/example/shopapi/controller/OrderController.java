package com.example.shopapi.controller;

import com.example.shopapi.document.Order;
import com.example.shopapi.dto.common.ApiResponse;
import com.example.shopapi.dto.common.PagedResponse;
import com.example.shopapi.dto.order.CreateOrderRequest;
import com.example.shopapi.dto.order.OrderResponse;
import com.example.shopapi.dto.order.UpdateOrderStatusRequest;
import com.example.shopapi.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order from cart")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable String id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get current user's orders")
    public ResponseEntity<PagedResponse<OrderResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderResponse> orders = orderService.getMyOrders(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PagedResponse.of(orders.getContent(), page, size, orders.getTotalElements()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Get all orders (Worker/Admin only)")
    public ResponseEntity<PagedResponse<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderResponse> orders = orderService.getAllOrders(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PagedResponse.of(orders.getContent(), page, size, orders.getTotalElements()));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Get orders by status (Worker/Admin only)")
    public ResponseEntity<PagedResponse<OrderResponse>> getOrdersByStatus(
            @PathVariable Order.OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderResponse> orders = orderService.getOrdersByStatus(status, PageRequest.of(page, size));
        return ResponseEntity.ok(PagedResponse.of(orders.getContent(), page, size, orders.getTotalElements()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Update order status (Worker/Admin only)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }
}

