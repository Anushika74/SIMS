package com.smartinventory.dto;

import java.math.BigDecimal;

/**
 * Aggregated sales figures for a product over a window (native query projection).
 * Products with no sales return zero quantity/revenue — used for dead-stock analysis.
 */
public interface ProductSalesProjection {
    Long getProductId();
    String getProductName();
    Long getTotalQuantity();
    BigDecimal getTotalRevenue();
}
