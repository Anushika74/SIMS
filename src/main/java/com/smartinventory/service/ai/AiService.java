package com.smartinventory.service.ai;

import com.smartinventory.dto.*;
import com.smartinventory.model.Product;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.SaleItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Orchestrates the AI features. It prefers the Python ML microservice (Flask +
 * scikit-learn) when it is reachable and transparently falls back to the
 * built-in {@link AiRuleEngine} otherwise, so the four AI features always work.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiRuleEngine ruleEngine;
    private final AiServiceClient client;
    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;

    @Value("${app.ai.window-days:90}")
    private int windowDays;

    public AiService(AiRuleEngine ruleEngine, AiServiceClient client,
                     ProductRepository productRepository, SaleItemRepository saleItemRepository) {
        this.ruleEngine = ruleEngine;
        this.client = client;
        this.productRepository = productRepository;
        this.saleItemRepository = saleItemRepository;
    }

    public boolean isMlServiceAvailable() {
        return client.isAvailable();
    }

    // ---- AI Feature 1: Restock prediction -----------------------------------
    public List<RestockPrediction> restockPredictions() {
        if (client.isAvailable()) {
            Optional<List<RestockPrediction>> ml = client.predictRestock(buildRestockPayload());
            if (ml.isPresent() && !ml.get().isEmpty()) {
                log.info("Restock predictions served by ML microservice.");
                List<RestockPrediction> list = ml.get();
                list.forEach(p -> { if (p.getSource() == null) p.setSource("ML model"); });
                return list;
            }
        }
        return ruleEngine.restockPredictions();
    }

    /** Only the items that actually need attention (urgency != OK). */
    public List<RestockPrediction> restockActionItems() {
        List<RestockPrediction> all = restockPredictions();
        List<RestockPrediction> action = new ArrayList<>();
        for (RestockPrediction p : all) {
            if (!"OK".equals(p.getUrgency())) action.add(p);
        }
        return action;
    }

    // ---- AI Feature 2: Movement analysis ------------------------------------
    public List<ProductMovement> movementAnalysis() {
        return ruleEngine.movementAnalysis();
    }

    public Map<String, Long> movementSummary() {
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("FAST", 0L);
        summary.put("SLOW", 0L);
        summary.put("DEAD", 0L);
        for (ProductMovement m : movementAnalysis()) {
            summary.merge(m.getCategory(), 1L, Long::sum);
        }
        return summary;
    }

    // ---- AI Feature 3: Trend analysis ---------------------------------------
    public TrendAnalysis trendAnalysis(int days, int forecastDays) {
        TrendAnalysis base = ruleEngine.trendAnalysis(days, forecastDays);
        if (client.isAvailable()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("labels", base.getLabels());
            payload.put("values", base.getValues());
            payload.put("forecastDays", forecastDays);
            Optional<TrendAnalysis> ml = client.forecastTrend(payload);
            if (ml.isPresent() && ml.get().getForecastValues() != null
                    && !ml.get().getForecastValues().isEmpty()) {
                log.info("Trend forecast served by ML microservice.");
                TrendAnalysis mlTa = ml.get();
                // Keep the rich history + weekday stats from the rule engine,
                // adopt the ML forecast + growth rate.
                base.setForecastLabels(mlTa.getForecastLabels());
                base.setForecastValues(mlTa.getForecastValues());
                if (mlTa.getGrowthRatePct() != 0) base.setGrowthRatePct(mlTa.getGrowthRatePct());
                base.setSource("ML model (scikit-learn)");
            }
        }
        return base;
    }

    // ---- AI Feature 4: Intelligent alerts -----------------------------------
    public List<Alert> alerts() {
        return ruleEngine.alerts();
    }

    public int activeAlertCount() {
        return alerts().size();
    }

    // -------------------------------------------------------------------------

    /** Builds the per-product daily sales series payload for the ML service. */
    private Map<String, Object> buildRestockPayload() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(windowDays - 1L);

        List<Map<String, Object>> products = new ArrayList<>();
        for (Product p : productRepository.findAll()) {
            // zero-filled daily series
            Map<String, Double> series = new LinkedHashMap<>();
            for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
                series.put(d.toString(), 0.0);
            }
            for (DailySalesProjection row : saleItemRepository
                    .dailyQuantityForProduct(p.getId(), start.atStartOfDay())) {
                if (series.containsKey(row.getDay())) {
                    series.put(row.getDay(), row.getTotal() == null ? 0.0 : row.getTotal().doubleValue());
                }
            }
            Map<String, Object> prod = new HashMap<>();
            prod.put("id", p.getId());
            prod.put("name", p.getName());
            prod.put("currentStock", p.getQuantity());
            prod.put("reorderLevel", p.getReorderLevel());
            prod.put("dailySales", new ArrayList<>(series.values()));
            products.add(prod);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("windowDays", windowDays);
        payload.put("products", products);
        return payload;
    }
}
