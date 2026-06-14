package com.smartinventory.dto;

import java.math.BigDecimal;

/** Aggregated sales for a single calendar day (native query projection). */
public interface DailySalesProjection {
    String getDay();          // yyyy-MM-dd
    BigDecimal getTotal();    // revenue for the day
    Long getTransactions();   // number of bills
}
