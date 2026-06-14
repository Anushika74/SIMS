package com.smartinventory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * AI Feature 4 — Intelligent Alert (low stock, overstock, sudden sales drop,
 * expiring product).
 */
@Getter
@Setter
@AllArgsConstructor
public class Alert {
    /** LOW_STOCK | OVERSTOCK | SALES_DROP | EXPIRY */
    private String type;
    /** danger | warning | info  (maps to Bootstrap colours) */
    private String severity;
    private String title;
    private String message;
    private String productName;
}
