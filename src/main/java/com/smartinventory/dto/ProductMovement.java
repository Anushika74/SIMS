package com.smartinventory.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * AI Feature 2 — Fast / Slow / Dead stock classification for a product.
 */
@Getter
@Setter
public class ProductMovement {
    private Long productId;
    private String productName;
    private long totalSold;
    private double dailyAvg;
    private BigDecimal revenue = BigDecimal.ZERO;
    /** Days since the product was last sold (null = never sold). */
    private Long daysSinceLastSale;
    /** FAST | SLOW | DEAD */
    private String category = "SLOW";
    private String message;
}
