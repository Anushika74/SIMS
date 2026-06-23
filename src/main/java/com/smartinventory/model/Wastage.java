package com.smartinventory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A wastage record — stock lost rather than sold (expired, damaged, spoiled,
 * stolen, etc.). Stores the monetary loss (quantity x cost price) so the system
 * can report and analyse losses.
 */
@Entity
@Table(name = "wastage")
@Getter
@Setter
@NoArgsConstructor
public class Wastage {

    public enum Reason { EXPIRED, DAMAGED, SPOILED, THEFT, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Reason reason = Reason.OTHER;

    @Column(length = 255)
    private String note;

    /** Financial loss = quantity * product cost price, captured at record time. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lossAmount = BigDecimal.ZERO;

    @Column(length = 50)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public Wastage(Product product, int quantity, Reason reason, String note,
                   BigDecimal lossAmount, String performedBy) {
        this.product = product;
        this.quantity = quantity;
        this.reason = reason;
        this.note = note;
        this.lossAmount = lossAmount;
        this.performedBy = performedBy;
    }
}
