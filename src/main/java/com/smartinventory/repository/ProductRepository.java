package com.smartinventory.repository;

import com.smartinventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Exact lookup by SKU / barcode (case-insensitive) — used by the scanner. */
    Optional<Product> findBySkuIgnoreCase(String sku);

    /** Case-insensitive search by name, SKU or category name. */
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(p.category.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Product> search(@Param("q") String q);

    /** Products at or below their reorder level (low stock). */
    @Query("SELECT p FROM Product p WHERE p.quantity <= p.reorderLevel ORDER BY p.quantity ASC")
    List<Product> findLowStock();

    /** Perishable products expiring on or before the given date. */
    @Query("SELECT p FROM Product p WHERE p.expiryDate IS NOT NULL AND p.expiryDate <= :date ORDER BY p.expiryDate ASC")
    List<Product> findExpiringBefore(@Param("date") LocalDate date);

    long countByQuantityLessThanEqual(int qty);
}
