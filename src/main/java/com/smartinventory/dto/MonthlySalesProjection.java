package com.smartinventory.dto;

import java.math.BigDecimal;

/** Aggregated sales for a single month (native query projection). */
public interface MonthlySalesProjection {
    String getMonth();        // yyyy-MM
    BigDecimal getTotal();    // revenue for the month
    Long getTransactions();   // number of bills
}
