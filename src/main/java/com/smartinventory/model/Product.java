package com.smartinventory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A product (SKU) sold by the shop. Holds pricing, current stock quantity and
 * the reorder level used by the inventory + AI alert logic.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 400)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    /** Selling price per unit. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /** Purchase / cost price per unit (used for revenue & margin analysis). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    /** Current quantity in stock. */
    @Column(nullable = false)
    private int quantity = 0;

    /** Reorder threshold — low-stock alert fires at or below this level. */
    @Column(nullable = false)
    private int reorderLevel = 10;

    @Column(length = 120)
    private String supplier;

    /** Optional expiry date for perishable goods. */
    private LocalDate expiryDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isLowStock() {
        return quantity <= reorderLevel;
    }

    public boolean isPerishable() {
        return expiryDate != null;
    }
}
