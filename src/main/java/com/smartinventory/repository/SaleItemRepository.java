package com.smartinventory.repository;

import com.smartinventory.dto.DailySalesProjection;
import com.smartinventory.dto.ProductSalesProjection;
import com.smartinventory.model.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    /**
     * Total quantity and revenue per product within a window. Uses a LEFT JOIN
     * from products so that products with NO sales still appear (quantity 0) —
     * essential for dead-stock detection. SUM(quantity) is CAST to SIGNED so it
     * maps cleanly to the {@code Long getTotalQuantity()} projection getter
     * (MySQL otherwise returns DECIMAL for SUM of an integer column).
     */
    @Query(value = "SELECT p.id AS productId, p.name AS productName, " +
            "CAST(COALESCE(SUM(si.quantity), 0) AS SIGNED) AS totalQuantity, " +
            "COALESCE(SUM(si.line_total), 0) AS totalRevenue " +
            "FROM products p " +
            "LEFT JOIN sale_items si ON si.product_id = p.id " +
            "LEFT JOIN sales s ON s.id = si.sale_id AND s.sale_date >= :from " +
            "GROUP BY p.id, p.name " +
            "ORDER BY totalQuantity DESC", nativeQuery = true)
    List<ProductSalesProjection> aggregateProductSalesSince(@Param("from") LocalDateTime from);

    /** Daily quantity sold for one product (for restock prediction / regression). */
    @Query(value = "SELECT DATE_FORMAT(s.sale_date, '%Y-%m-%d') AS day, " +
            "COALESCE(SUM(si.quantity), 0) AS total, CAST(0 AS SIGNED) AS transactions " +
            "FROM sale_items si JOIN sales s ON s.id = si.sale_id " +
            "WHERE si.product_id = :productId AND s.sale_date >= :from " +
            "GROUP BY DATE_FORMAT(s.sale_date, '%Y-%m-%d') ORDER BY day", nativeQuery = true)
    List<DailySalesProjection> dailyQuantityForProduct(
            @Param("productId") Long productId, @Param("from") LocalDateTime from);

    /** Last time a product was sold (NULL if never). JPQL keeps clean LocalDateTime mapping. */
    @Query("SELECT MAX(si.sale.saleDate) FROM SaleItem si WHERE si.product.id = :productId")
    LocalDateTime lastSoldDate(@Param("productId") Long productId);
}
