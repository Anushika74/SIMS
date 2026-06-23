package com.smartinventory.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * AI predicted-wastage result: a perishable product likely to expire before it
 * sells, with the estimated units wasted and money lost.
 */
@Getter
@Setter
public class WastagePrediction {
    private Long productId;
    private String productName;
    private int currentStock;
    private Integer daysToExpiry;       // negative = already expired
    private long projectedSales;        // expected units sold before expiry
    private long predictedWasteUnits;
    private BigDecimal estimatedLoss = BigDecimal.ZERO;
    private String message;
}
