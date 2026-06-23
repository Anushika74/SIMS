package com.smartinventory.repository;

import com.smartinventory.dto.LabelValue;
import com.smartinventory.model.Wastage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface WastageRepository extends JpaRepository<Wastage, Long> {

    List<Wastage> findTop20ByOrderByTimestampDesc();

    @Query("SELECT COALESCE(SUM(w.lossAmount), 0) FROM Wastage w WHERE w.timestamp BETWEEN :from AND :to")
    BigDecimal sumLossBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(w.lossAmount), 0) FROM Wastage w")
    BigDecimal sumLossAll();

    /** Loss grouped by reason. */
    @Query(value = "SELECT reason AS label, COALESCE(SUM(loss_amount), 0) AS total, " +
            "CAST(COALESCE(SUM(quantity), 0) AS SIGNED) AS qty " +
            "FROM wastage GROUP BY reason ORDER BY total DESC", nativeQuery = true)
    List<LabelValue> aggregateByReason();

    /** Loss grouped by month (for the trend chart). */
    @Query(value = "SELECT DATE_FORMAT(timestamp, '%Y-%m') AS label, " +
            "COALESCE(SUM(loss_amount), 0) AS total, " +
            "CAST(COALESCE(SUM(quantity), 0) AS SIGNED) AS qty " +
            "FROM wastage GROUP BY DATE_FORMAT(timestamp, '%Y-%m') ORDER BY label", nativeQuery = true)
    List<LabelValue> aggregateByMonth();

    /** Products with the highest wastage loss. */
    @Query(value = "SELECT p.name AS label, COALESCE(SUM(w.loss_amount), 0) AS total, " +
            "CAST(COALESCE(SUM(w.quantity), 0) AS SIGNED) AS qty " +
            "FROM wastage w JOIN products p ON p.id = w.product_id " +
            "GROUP BY p.name ORDER BY total DESC", nativeQuery = true)
    List<LabelValue> topWastedProducts();
}
