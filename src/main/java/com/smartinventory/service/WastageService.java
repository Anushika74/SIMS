package com.smartinventory.service;

import com.smartinventory.dto.LabelValue;
import com.smartinventory.dto.WastageRequest;
import com.smartinventory.model.Product;
import com.smartinventory.model.StockMovement;
import com.smartinventory.model.Wastage;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.StockMovementRepository;
import com.smartinventory.repository.WastageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Records wastage (stock lost not sold), reduces stock, captures the monetary
 * loss, and provides wastage reports.
 */
@Service
public class WastageService {

    private final WastageRepository wastageRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;

    public WastageService(WastageRepository wastageRepository, ProductRepository productRepository,
                          StockMovementRepository movementRepository) {
        this.wastageRepository = wastageRepository;
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public Wastage record(WastageRequest req, String user) {
        if (req.getProductId() == null || req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new IllegalArgumentException("Select a product and a quantity greater than zero.");
        }
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        if (req.getQuantity() > product.getQuantity()) {
            throw new IllegalArgumentException("Cannot waste " + req.getQuantity() + " units of "
                    + product.getName() + " — only " + product.getQuantity() + " in stock.");
        }

        Wastage.Reason reason;
        try {
            reason = Wastage.Reason.valueOf(req.getReason() == null ? "OTHER" : req.getReason().toUpperCase());
        } catch (IllegalArgumentException ex) {
            reason = Wastage.Reason.OTHER;
        }

        BigDecimal loss = product.getCostPrice().multiply(BigDecimal.valueOf(req.getQuantity()));

        // reduce stock
        product.setQuantity(product.getQuantity() - req.getQuantity());
        productRepository.save(product);

        // log audit movement
        movementRepository.save(new StockMovement(product, StockMovement.Type.WASTE, req.getQuantity(),
                "Wastage: " + reason, user));

        // record the wastage with its loss value
        return wastageRepository.save(new Wastage(product, req.getQuantity(), reason, req.getNote(), loss, user));
    }

    // ---- reports ------------------------------------------------------------

    public List<Wastage> recent() {
        return wastageRepository.findTop20ByOrderByTimestampDesc();
    }

    public BigDecimal totalLoss() {
        return wastageRepository.sumLossAll();
    }

    public BigDecimal monthLoss() {
        LocalDateTime start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return wastageRepository.sumLossBetween(start, LocalDateTime.now());
    }

    public long count() {
        return wastageRepository.count();
    }

    public List<LabelValue> byReason() {
        return wastageRepository.aggregateByReason();
    }

    public List<LabelValue> byMonth() {
        return wastageRepository.aggregateByMonth();
    }

    public List<LabelValue> topWastedProducts() {
        return wastageRepository.topWastedProducts();
    }
}
