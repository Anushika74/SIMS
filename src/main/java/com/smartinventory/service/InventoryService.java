package com.smartinventory.service;

import com.smartinventory.model.Product;
import com.smartinventory.model.StockMovement;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Stock add / adjust operations with an audit trail of movements. */
@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;

    public InventoryService(ProductRepository productRepository, StockMovementRepository movementRepository) {
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
    }

    /** Increase stock (a purchase / restock) and log a movement. */
    @Transactional
    public Product addStock(Long productId, int quantity, String reason, String user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        product.setQuantity(product.getQuantity() + quantity);
        productRepository.save(product);
        movementRepository.save(new StockMovement(product, StockMovement.Type.IN, quantity,
                reason == null ? "Stock added" : reason, user));
        return product;
    }

    /** Set the stock to an exact value (manual correction) and log the delta. */
    @Transactional
    public Product setStock(Long productId, int newQuantity, String reason, String user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        int delta = newQuantity - product.getQuantity();
        product.setQuantity(Math.max(0, newQuantity));
        productRepository.save(product);
        movementRepository.save(new StockMovement(product, StockMovement.Type.ADJUST, Math.abs(delta),
                reason == null ? "Manual adjustment" : reason, user));
        return product;
    }

    /** Reduce stock for a sale and log an OUT movement. */
    @Transactional
    public void reduceStockForSale(Product product, int quantity, String user) {
        product.setQuantity(Math.max(0, product.getQuantity() - quantity));
        productRepository.save(product);
        movementRepository.save(new StockMovement(product, StockMovement.Type.OUT, quantity, "Sale", user));
    }

    public List<Product> availableStock() {
        return productRepository.findAll();
    }

    public List<Product> lowStock() {
        return productRepository.findLowStock();
    }

    public List<StockMovement> recentMovements() {
        return movementRepository.findTop20ByOrderByTimestampDesc();
    }
}
