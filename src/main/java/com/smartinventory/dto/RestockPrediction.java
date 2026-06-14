package com.smartinventory.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * AI Feature 1 — Smart Restock Prediction result for a single product.
 */
@Getter
@Setter
public class RestockPrediction {
    private Long productId;
    private String productName;
    private int currentStock;
    private double avgDailySales;
    /** Estimated days until stock reaches zero (null = effectively never at current rate). */
    private Integer daysToStockout;
    private int recommendedRestockQty;
    /** HIGH | MEDIUM | LOW | OK */
    private String urgency = "OK";
    private String message;
    /** "ML model" when served by the Flask/scikit-learn service, otherwise "rule-based". */
    private String source = "rule-based";
}
