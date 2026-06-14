package com.smartinventory.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Headline figures shown on the dashboard. */
@Getter
@Setter
public class DashboardStats {
    private long totalProducts;
    private long lowStockCount;
    private long todaysTransactions;
    private BigDecimal todaysRevenue = BigDecimal.ZERO;
    private BigDecimal monthRevenue = BigDecimal.ZERO;
    private long totalSales;
    private int activeAlerts;
}
