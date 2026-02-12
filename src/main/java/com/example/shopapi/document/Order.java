package com.example.shopapi.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "orders")
@CompoundIndex(name = "user_status_idx", def = "{'userId': 1, 'status': 1}")
@CompoundIndex(name = "user_created_idx", def = "{'userId': 1, 'createdAt': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    private String id;

    @Indexed
    private Long userId;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal subtotal;

    private BigDecimal tax;

    private BigDecimal shippingCost;

    private BigDecimal discount;

    private BigDecimal totalAmount;

    @Indexed
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    private ShippingAddress shippingAddress;

    private BillingAddress billingAddress;

    @Indexed
    private String paymentId;

    private String paymentSessionId;

    private String paymentMethod;

    private String trackingNumber;

    private String notes;

    private String cancellationReason;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime paidAt;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime cancelledAt;

    public enum OrderStatus {
        CREATED,
        PENDING_PAYMENT,
        PAID,
        FAILED,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        REFUNDED
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItem {
        private String productId;
        private String productName;
        private String productSku;
        private BigDecimal price;
        private Integer quantity;
        private String imageUrl;

        public BigDecimal getSubtotal() {
            if (price != null && quantity != null) {
                return price.multiply(BigDecimal.valueOf(quantity));
            }
            return BigDecimal.ZERO;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShippingAddress {
        private String fullName;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String phone;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BillingAddress {
        private String fullName;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }

    public int getTotalItems() {
        return items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    public void calculateTotals() {
        this.subtotal = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (this.tax == null) this.tax = BigDecimal.ZERO;
        if (this.shippingCost == null) this.shippingCost = BigDecimal.ZERO;
        if (this.discount == null) this.discount = BigDecimal.ZERO;

        this.totalAmount = this.subtotal
                .add(this.tax)
                .add(this.shippingCost)
                .subtract(this.discount);
    }
}

