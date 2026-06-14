package com.smartinventory.repository;

import com.smartinventory.dto.DailySalesProjection;
import com.smartinventory.dto.MonthlySalesProjection;
import com.smartinventory.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findBySaleDateBetweenOrderBySaleDateDesc(LocalDateTime from, LocalDateTime to);

    List<Sale> findTop10ByOrderBySaleDateDesc();

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.saleDate BETWEEN :from AND :to")
    java.math.BigDecimal sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countBySaleDateBetween(LocalDateTime from, LocalDateTime to);

    /** Revenue and transaction count grouped by calendar day. */
    @Query(value = "SELECT DATE_FORMAT(s.sale_date, '%Y-%m-%d') AS day, " +
            "SUM(s.total_amount) AS total, COUNT(*) AS transactions " +
            "FROM sales s WHERE s.sale_date >= :from " +
            "GROUP BY DATE_FORMAT(s.sale_date, '%Y-%m-%d') ORDER BY day", nativeQuery = true)
    List<DailySalesProjection> findDailySales(@Param("from") LocalDateTime from);

    /** Revenue and transaction count grouped by month. */
    @Query(value = "SELECT DATE_FORMAT(s.sale_date, '%Y-%m') AS month, " +
            "SUM(s.total_amount) AS total, COUNT(*) AS transactions " +
            "FROM sales s GROUP BY DATE_FORMAT(s.sale_date, '%Y-%m') ORDER BY month", nativeQuery = true)
    List<MonthlySalesProjection> findMonthlySales();
}
