package com.smartinventory.service.ai;

import com.smartinventory.dto.*;
import com.smartinventory.model.Product;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.SaleItemRepository;
import com.smartinventory.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Built-in, fully explainable AI engine. It powers all four AI features using
 * transparent statistical rules over the historical sales data:
 *
 * <ol>
 *   <li><b>Restock prediction</b> — days-to-stockout = currentStock / avgDailySales;
 *       recommended quantity covers a target stock horizon.</li>
 *   <li><b>Movement analysis</b> — products ranked by units sold and recency are
 *       labelled FAST / SLOW / DEAD.</li>
 *   <li><b>Trend analysis</b> — least-squares linear regression on the daily
 *       revenue series gives the growth rate and a weekday-seasonal forecast.</li>
 *   <li><b>Intelligent alerts</b> — low stock, overstock, sudden sales drop and expiry.</li>
 * </ol>
 *
 * This engine is the fallback used whenever the Python ML microservice is offline,
 * so the system always produces working AI output.
 */
@Component
public class AiRuleEngine {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;

    @Value("${app.ai.window-days:90}")
    private int windowDays;
    @Value("${app.alerts.overstock-days:60}")
    private int overstockDays;
    @Value("${app.alerts.dead-stock-days:30}")
    private int deadStockDays;
    @Value("${app.alerts.expiry-warning-days:14}")
    private int expiryWarningDays;

    /** Target number of days of stock cover when recommending a restock quantity. */
    private static final int TARGET_COVER_DAYS = 30;

    public AiRuleEngine(ProductRepository productRepository, SaleRepository saleRepository,
                        SaleItemRepository saleItemRepository) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
    }

    // ------------------------------------------------------------------
    // AI Feature 1 — Smart Restock Prediction
    // ------------------------------------------------------------------
    public List<RestockPrediction> restockPredictions() {
        LocalDateTime from = LocalDate.now().minusDays(windowDays).atStartOfDay();
        Map<Long, Long> soldByProduct = new HashMap<>();
        for (ProductSalesProjection row : saleItemRepository.aggregateProductSalesSince(from)) {
            soldByProduct.put(row.getProductId(), row.getTotalQuantity() == null ? 0L : row.getTotalQuantity());
        }

        List<RestockPrediction> result = new ArrayList<>();
        for (Product p : productRepository.findAll()) {
            long sold = soldByProduct.getOrDefault(p.getId(), 0L);
            double avgDaily = sold / (double) windowDays;

            RestockPrediction pr = new RestockPrediction();
            pr.setProductId(p.getId());
            pr.setProductName(p.getName());
            pr.setCurrentStock(p.getQuantity());
            pr.setAvgDailySales(round2(avgDaily));

            if (avgDaily > 0.0001) {
                int days = (int) Math.floor(p.getQuantity() / avgDaily);
                pr.setDaysToStockout(days);
                int target = (int) Math.ceil(avgDaily * TARGET_COVER_DAYS);
                int recommend = Math.max(0, target - p.getQuantity());
                pr.setRecommendedRestockQty(recommend);
                pr.setUrgency(urgencyFor(days, p));
                pr.setMessage(buildRestockMessage(p, days, recommend));
            } else {
                pr.setDaysToStockout(null);
                pr.setRecommendedRestockQty(0);
                pr.setUrgency("OK");
                pr.setMessage(p.getName() + " has no recent sales — no restock needed.");
            }
            result.add(pr);
        }
        // Most urgent first
        result.sort(Comparator.comparingInt((RestockPrediction r) -> urgencyRank(r.getUrgency()))
                .thenComparing(r -> r.getDaysToStockout() == null ? Integer.MAX_VALUE : r.getDaysToStockout()));
        return result;
    }

    private String buildRestockMessage(Product p, int days, int recommend) {
        if (recommend <= 0) {
            return p.getName() + " stock is healthy (~" + days + " days of cover).";
        }
        return String.format("%s may run out within %d days. Recommended restock quantity: %d units.",
                p.getName(), days, recommend);
    }

    private String urgencyFor(int days, Product p) {
        if (days <= 3 || p.getQuantity() <= p.getReorderLevel()) return "HIGH";
        if (days <= 7) return "MEDIUM";
        if (days <= 14) return "LOW";
        return "OK";
    }

    private int urgencyRank(String u) {
        return switch (u) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            case "LOW" -> 2;
            default -> 3;
        };
    }

    // ------------------------------------------------------------------
    // AI Feature 2 — Fast / Slow / Dead movement analysis
    // ------------------------------------------------------------------
    public List<ProductMovement> movementAnalysis() {
        LocalDateTime from = LocalDate.now().minusDays(windowDays).atStartOfDay();
        List<ProductSalesProjection> rows = saleItemRepository.aggregateProductSalesSince(from);

        List<ProductMovement> movements = new ArrayList<>();
        for (ProductSalesProjection row : rows) {
            ProductMovement m = new ProductMovement();
            m.setProductId(row.getProductId());
            m.setProductName(row.getProductName());
            long sold = row.getTotalQuantity() == null ? 0L : row.getTotalQuantity();
            m.setTotalSold(sold);
            m.setDailyAvg(round2(sold / (double) windowDays));
            m.setRevenue(row.getTotalRevenue() == null ? BigDecimal.ZERO : row.getTotalRevenue());

            LocalDateTime lastSold = saleItemRepository.lastSoldDate(row.getProductId());
            if (lastSold == null) {
                m.setDaysSinceLastSale(null);
            } else {
                m.setDaysSinceLastSale(Duration.between(lastSold, LocalDateTime.now()).toDays());
            }
            movements.add(m);
        }

        // Classify: DEAD if never sold or stale; among the rest the busiest 25% are FAST.
        List<ProductMovement> active = new ArrayList<>();
        for (ProductMovement m : movements) {
            boolean dead = m.getTotalSold() == 0
                    || m.getDaysSinceLastSale() == null
                    || m.getDaysSinceLastSale() > deadStockDays;
            if (dead) {
                m.setCategory("DEAD");
                long d = m.getDaysSinceLastSale() == null ? -1 : m.getDaysSinceLastSale();
                m.setMessage(d < 0
                        ? m.getProductName() + " has never sold — dead stock."
                        : m.getProductName() + " has not sold for " + d + " days — dead stock.");
            } else {
                active.add(m);
            }
        }
        active.sort(Comparator.comparingLong(ProductMovement::getTotalSold).reversed());
        int fastCount = Math.max(1, (int) Math.ceil(active.size() * 0.25));
        for (int i = 0; i < active.size(); i++) {
            ProductMovement m = active.get(i);
            if (i < fastCount) {
                m.setCategory("FAST");
                m.setMessage(m.getProductName() + " is a fast-moving product (" + m.getTotalSold() + " units sold).");
            } else {
                m.setCategory("SLOW");
                m.setMessage(m.getProductName() + " is slow-moving (" + m.getTotalSold() + " units sold).");
            }
        }
        return movements;
    }

    // ------------------------------------------------------------------
    // AI Feature 3 — Sales Trend Analysis + forecast
    // ------------------------------------------------------------------
    public TrendAnalysis trendAnalysis(int days, int forecastDays) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);

        // Build a date->revenue map seeded with zeros so gaps are not skipped.
        Map<String, Double> revenueByDay = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            revenueByDay.put(d.toString(), 0.0);
        }
        for (DailySalesProjection row : saleRepository.findDailySales(start.atStartOfDay())) {
            if (revenueByDay.containsKey(row.getDay())) {
                revenueByDay.put(row.getDay(), row.getTotal() == null ? 0.0 : row.getTotal().doubleValue());
            }
        }

        TrendAnalysis ta = new TrendAnalysis();
        ta.getLabels().addAll(revenueByDay.keySet());
        ta.getValues().addAll(revenueByDay.values());

        // Weekday vs weekend averages
        double[] weekdaySums = new double[2]; // [0]=weekday, [1]=weekend
        int[] weekdayCounts = new int[2];
        double[] byDow = new double[7];
        int[] dowCounts = new int[7];
        int idx = 0;
        for (Map.Entry<String, Double> e : revenueByDay.entrySet()) {
            LocalDate d = LocalDate.parse(e.getKey());
            DayOfWeek dow = d.getDayOfWeek();
            boolean weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
            weekdaySums[weekend ? 1 : 0] += e.getValue();
            weekdayCounts[weekend ? 1 : 0]++;
            byDow[dow.getValue() - 1] += e.getValue();
            dowCounts[dow.getValue() - 1]++;
            idx++;
        }
        ta.setWeekdayAverage(round2(safeDiv(weekdaySums[0], weekdayCounts[0])));
        ta.setWeekendAverage(round2(safeDiv(weekdaySums[1], weekdayCounts[1])));

        // Linear regression for growth rate
        double[] ys = ta.getValues().stream().mapToDouble(Double::doubleValue).toArray();
        double[] lr = linearRegression(ys);
        double slope = lr[0];
        double intercept = lr[1];
        double mean = Arrays.stream(ys).average().orElse(0);
        ta.setGrowthRatePct(round2(mean == 0 ? 0 : (slope / mean) * 100));

        // Forecast: blend the regression trend with weekday seasonality
        double[] dowAvg = new double[7];
        for (int i = 0; i < 7; i++) dowAvg[i] = safeDiv(byDow[i], dowCounts[i]);
        for (int k = 1; k <= forecastDays; k++) {
            LocalDate fd = today.plusDays(k);
            double trend = intercept + slope * (ys.length + k - 1);
            double seasonal = dowAvg[fd.getDayOfWeek().getValue() - 1];
            double forecast = Math.max(0, 0.5 * trend + 0.5 * seasonal);
            ta.getForecastLabels().add(fd.toString());
            ta.getForecastValues().add(round2(forecast));
        }

        buildTrendInsights(ta);
        return ta;
    }

    private void buildTrendInsights(TrendAnalysis ta) {
        List<String> insights = ta.getInsights();
        if (ta.getWeekendAverage() > ta.getWeekdayAverage() * 1.1) {
            double pct = ta.getWeekdayAverage() == 0 ? 100
                    : (ta.getWeekendAverage() - ta.getWeekdayAverage()) / ta.getWeekdayAverage() * 100;
            insights.add(String.format("Sales increase on weekends (about %.0f%% higher than weekdays).", pct));
        } else if (ta.getWeekdayAverage() > ta.getWeekendAverage() * 1.1) {
            insights.add("Weekdays sell better than weekends for this shop.");
        }
        if (ta.getGrowthRatePct() > 1) {
            insights.add(String.format("Overall sales are trending upward (~%.1f%% per day).", ta.getGrowthRatePct()));
        } else if (ta.getGrowthRatePct() < -1) {
            insights.add(String.format("Overall sales are trending downward (~%.1f%% per day). Consider promotions.",
                    Math.abs(ta.getGrowthRatePct())));
        } else {
            insights.add("Overall sales are stable over the analysed period.");
        }
        if (!ta.getForecastValues().isEmpty()) {
            double sum = ta.getForecastValues().stream().mapToDouble(Double::doubleValue).sum();
            insights.add(String.format("Forecast revenue for the next %d days: ~%.0f.",
                    ta.getForecastValues().size(), sum));
        }
    }

    // ------------------------------------------------------------------
    // AI Feature 4 — Intelligent Alerts
    // ------------------------------------------------------------------
    public List<Alert> alerts() {
        List<Alert> alerts = new ArrayList<>();
        LocalDateTime from = LocalDate.now().minusDays(windowDays).atStartOfDay();
        Map<Long, Long> soldByProduct = new HashMap<>();
        for (ProductSalesProjection row : saleItemRepository.aggregateProductSalesSince(from)) {
            soldByProduct.put(row.getProductId(), row.getTotalQuantity() == null ? 0L : row.getTotalQuantity());
        }

        for (Product p : productRepository.findAll()) {
            // Low stock
            if (p.getQuantity() <= p.getReorderLevel()) {
                alerts.add(new Alert("LOW_STOCK", "danger", "Low Stock",
                        p.getName() + " is low (" + p.getQuantity() + " left, reorder at " + p.getReorderLevel() + ").",
                        p.getName()));
            }
            // Overstock
            double avgDaily = soldByProduct.getOrDefault(p.getId(), 0L) / (double) windowDays;
            if (avgDaily > 0.0001) {
                int cover = (int) Math.floor(p.getQuantity() / avgDaily);
                if (cover > overstockDays) {
                    alerts.add(new Alert("OVERSTOCK", "info", "Overstock",
                            p.getName() + " has ~" + cover + " days of stock (overstocked). Slow down purchasing.",
                            p.getName()));
                }
            }
            // Expiry
            if (p.getExpiryDate() != null) {
                long daysToExpiry = Duration.between(LocalDateTime.now(),
                        p.getExpiryDate().atStartOfDay()).toDays();
                if (daysToExpiry < 0) {
                    alerts.add(new Alert("EXPIRY", "danger", "Expired",
                            p.getName() + " expired " + Math.abs(daysToExpiry) + " days ago — remove from shelf.",
                            p.getName()));
                } else if (daysToExpiry <= expiryWarningDays) {
                    alerts.add(new Alert("EXPIRY", "warning", "Expiring Soon",
                            p.getName() + " expires in " + daysToExpiry + " days.", p.getName()));
                }
            }
        }

        // Sudden sales drop (overall, last 7 days vs previous 7 days)
        BigDecimal last7 = saleRepository.sumRevenueBetween(
                LocalDate.now().minusDays(7).atStartOfDay(), LocalDateTime.now());
        BigDecimal prev7 = saleRepository.sumRevenueBetween(
                LocalDate.now().minusDays(14).atStartOfDay(), LocalDate.now().minusDays(7).atStartOfDay());
        if (prev7.doubleValue() > 0) {
            double drop = (prev7.doubleValue() - last7.doubleValue()) / prev7.doubleValue() * 100;
            if (drop >= 30) {
                alerts.add(new Alert("SALES_DROP", "warning", "Sudden Sales Drop",
                        String.format("Sales fell %.0f%% this week vs last week. Investigate demand.", drop), null));
            }
        }
        return alerts;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Ordinary least squares; returns {slope, intercept}. */
    private double[] linearRegression(double[] y) {
        int n = y.length;
        if (n < 2) return new double[]{0, n == 1 ? y[0] : 0};
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += y[i];
            sumXY += i * y[i];
            sumXX += (double) i * i;
        }
        double denom = (n * sumXX - sumX * sumX);
        double slope = denom == 0 ? 0 : (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;
        return new double[]{slope, intercept};
    }

    private double safeDiv(double a, int b) {
        return b == 0 ? 0 : a / b;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
