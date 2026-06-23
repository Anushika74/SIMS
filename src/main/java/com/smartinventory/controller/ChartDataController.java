package com.smartinventory.controller;

import com.smartinventory.dto.*;
import com.smartinventory.service.ReportService;
import com.smartinventory.service.ai.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * JSON endpoints consumed by Chart.js on the front-end. Provides data for the
 * four required charts (Sales Trend, Product Performance, Revenue Analysis,
 * AI Prediction) plus the movement breakdown.
 */
@RestController
@RequestMapping("/api/charts")
public class ChartDataController {

    private final ReportService reportService;
    private final AiService aiService;
    private final com.smartinventory.service.WastageService wastageService;

    public ChartDataController(ReportService reportService, AiService aiService,
                               com.smartinventory.service.WastageService wastageService) {
        this.reportService = reportService;
        this.aiService = aiService;
        this.wastageService = wastageService;
    }

    /** Chart 1 — Sales Trend (history + AI forecast). */
    @GetMapping("/sales-trend")
    public Map<String, Object> salesTrend(@RequestParam(defaultValue = "30") int days,
                                          @RequestParam(defaultValue = "7") int forecast) {
        TrendAnalysis t = aiService.trendAnalysis(days, forecast);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("labels", t.getLabels());
        m.put("values", t.getValues());
        m.put("forecastLabels", t.getForecastLabels());
        m.put("forecastValues", t.getForecastValues());
        m.put("weekdayAverage", t.getWeekdayAverage());
        m.put("weekendAverage", t.getWeekendAverage());
        m.put("source", t.getSource());
        return m;
    }

    /** Chart 2 — Product Performance (top sellers by units). */
    @GetMapping("/product-performance")
    public Map<String, Object> productPerformance(@RequestParam(defaultValue = "10") int top) {
        List<ProductMovement> movements = new ArrayList<>(aiService.movementAnalysis());
        movements.sort(Comparator.comparingLong(ProductMovement::getTotalSold).reversed());
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        movements.stream().limit(top).forEach(m -> {
            labels.add(m.getProductName());
            values.add(m.getTotalSold());
        });
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("labels", labels);
        m.put("values", values);
        return m;
    }

    /** Chart 3 — Revenue Analysis (monthly revenue). */
    @GetMapping("/revenue")
    public Map<String, Object> revenue() {
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (MonthlySalesProjection row : reportService.monthlySales()) {
            labels.add(row.getMonth());
            values.add(row.getTotal() == null ? 0.0 : row.getTotal().doubleValue());
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("labels", labels);
        m.put("values", values);
        return m;
    }

    /** Chart 4 — AI Prediction (top products by predicted days-to-stockout, ascending = most urgent). */
    @GetMapping("/restock")
    public Map<String, Object> restock(@RequestParam(defaultValue = "10") int top) {
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        aiService.restockPredictions().stream()
                .filter(p -> p.getDaysToStockout() != null)
                .limit(top)
                .forEach(p -> {
                    labels.add(p.getProductName());
                    values.add(p.getDaysToStockout());
                });
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("labels", labels);
        m.put("values", values);
        return m;
    }

    /** Movement breakdown (Fast / Slow / Dead) for the AI insights doughnut. */
    @GetMapping("/movement")
    public Map<String, Object> movement() {
        Map<String, Long> summary = aiService.movementSummary();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("labels", new ArrayList<>(summary.keySet()));
        m.put("values", new ArrayList<>(summary.values()));
        return m;
    }

    /** Wastage loss grouped by reason (doughnut). */
    @GetMapping("/wastage-by-reason")
    public Map<String, Object> wastageByReason() {
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        wastageService.byReason().forEach(r -> {
            labels.add(r.getLabel());
            values.add(r.getTotal() == null ? 0.0 : r.getTotal().doubleValue());
        });
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("labels", labels);
        m.put("values", values);
        return m;
    }

    /** Wastage loss trend by month (bar). */
    @GetMapping("/wastage-trend")
    public Map<String, Object> wastageTrend() {
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        wastageService.byMonth().forEach(r -> {
            labels.add(r.getLabel());
            values.add(r.getTotal() == null ? 0.0 : r.getTotal().doubleValue());
        });
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("labels", labels);
        m.put("values", values);
        return m;
    }
}
