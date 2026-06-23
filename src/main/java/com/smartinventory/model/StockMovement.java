package com.smartinventory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Audit record of every stock change (purchase in, sale out, manual adjustment).
 * Provides the historical demand data the AI module uses.
 */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
public class StockMovement {

    public enum Type { IN, OUT, ADJUST, WASTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;

    /** Positive quantity moved (direction implied by {@link #type}). */
    @Column(nullable = false)
    private int quantity;

    @Column(length = 200)
    private String reason;

    @Column(length = 50)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public StockMovement(Product product, Type type, int quantity, String reason, String performedBy) {
        this.product = product;
        this.type = type;
        this.quantity = quantity;
        this.reason = reason;
        this.performedBy = performedBy;
    }
}
