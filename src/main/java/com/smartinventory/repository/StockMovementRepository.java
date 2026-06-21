package com.smartinventory.repository;

import com.smartinventory.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findTop20ByOrderByTimestampDesc();
    List<StockMovement> findByProductIdOrderByTimestampDesc(Long productId);

    /** Remove a product's audit-trail movements (safe to delete with the product). */
    void deleteByProductId(Long productId);
}
