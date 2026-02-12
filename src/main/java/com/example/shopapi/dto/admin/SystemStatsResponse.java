package com.example.shopapi.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsResponse {

    private long totalUsers;
    private long totalWorkers;
    private long totalAdmins;
    private long activeUsers;
    private long newUsersThisMonth;

    private long totalProducts;
    private long activeProducts;
    private long outOfStockProducts;

    private long totalOrders;
    private long pendingOrders;
    private long completedOrders;
    private long failedOrders;

    private long successfulPayments;
    private long failedPayments;
    private BigDecimal totalRevenue;
    private BigDecimal revenueThisMonth;

    private long openTickets;
    private long resolvedTickets;
    private long totalTickets;
}

