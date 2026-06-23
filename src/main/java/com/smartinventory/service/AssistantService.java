package com.smartinventory.service;

import com.smartinventory.dto.*;
import com.smartinventory.model.Product;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.service.ai.AiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Rule-based AI Assistant (chatbot). It understands natural-language questions
 * about the shop by matching keywords, then answers using live data from the
 * existing services and the AI engine (restock, movement, trend, alerts).
 *
 * <p>This is a transparent, explainable NLP-lite assistant — easy to demonstrate
 * and to describe in the viva, and it always works (no external LLM needed).</p>
 */
@Service
public class AssistantService {

    private final AiService aiService;
    private final ReportService reportService;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final WastageService wastageService;

    @Value("${app.currency.symbol:Rs.}")
    private String currency;
    @Value("${app.alerts.expiry-warning-days:14}")
    private int expiryWarningDays;

    public AssistantService(AiService aiService, ReportService reportService,
                            ProductService productService, ProductRepository productRepository,
                            WastageService wastageService) {
        this.aiService = aiService;
        this.reportService = reportService;
        this.productService = productService;
        this.productRepository = productRepository;
        this.wastageService = wastageService;
    }

    public Map<String, Object> ask(String question) {
        String q = question == null ? "" : question.toLowerCase().trim();
        return Map.of("question", question == null ? "" : question, "answer", answer(q));
    }

    private String answer(String q) {
        if (q.isBlank() || contains(q, "hello", "hi ", "hey", "help", "what can you", "who are you"))
            return help();
        if (contains(q, "restock", "run out", "stockout", "stock out", "order more", "reorder"))
            return restock();
        if (contains(q, "low stock", "running low", "low on", "almost out"))
            return lowStock();
        if (contains(q, "dead", "not sold", "not selling", "obsolete", "no sales"))
            return dead();
        if (contains(q, "fast", "best sell", "top sell", "best-selling", "popular", "most sold", "top product"))
            return fast();
        if (contains(q, "slow"))
            return slow();
        if (contains(q, "expire", "expiry", "expiring", "perish"))
            return expiring();
        if (contains(q, "today"))
            return today();
        if (contains(q, "month"))
            return month();
        if (contains(q, "trend", "forecast", "weekend", "prediction", "future"))
            return trend();
        if (contains(q, "how many product", "total product", "number of product", "product count", "how many item"))
            return productCount();
        if (contains(q, "wastage", "waste", "spoil", "write off", "loss"))
            return wastage();
        if (contains(q, "alert", "warning"))
            return alerts();
        if (contains(q, "revenue", "sales", "income", "earn", "profit", "money"))
            return today();
        return fallback();
    }

    // ---- intent answers -----------------------------------------------------

    private String help() {
        return "👋 Hi! I'm your inventory assistant. Ask me things like:"
                + "<ul class='mb-0'>"
                + "<li>What should I <b>restock</b>?</li>"
                + "<li>Show <b>low stock</b> products</li>"
                + "<li>What are my <b>fast-moving</b> / <b>dead stock</b> products?</li>"
                + "<li>Which products are <b>expiring</b> soon?</li>"
                + "<li>What is <b>today's revenue</b> / <b>this month's</b> sales?</li>"
                + "<li>What's the <b>sales trend</b>?</li>"
                + "<li>Show me the <b>alerts</b></li>"
                + "<li>How much <b>wastage</b> this month?</li>"
                + "</ul>";
    }

    private String restock() {
        List<RestockPrediction> items = aiService.restockActionItems();
        if (items.isEmpty()) return "✅ Good news — no products need restocking right now.";
        StringBuilder sb = new StringBuilder("🔮 <b>Restock suggestions</b> (most urgent first):<ul class='mb-0'>");
        items.stream().limit(8).forEach(r -> sb.append("<li><b>").append(r.getProductName()).append("</b> — ")
                .append(r.getDaysToStockout() != null ? r.getDaysToStockout() + " days of stock left" : "low")
                .append(", restock <b>").append(r.getRecommendedRestockQty()).append("</b> units</li>"));
        return sb.append("</ul>").toString();
    }

    private String lowStock() {
        List<Product> low = productService.lowStock();
        if (low.isEmpty()) return "✅ No products are below their reorder level.";
        StringBuilder sb = new StringBuilder("⚠️ <b>Low-stock products</b>:<ul class='mb-0'>");
        low.stream().limit(10).forEach(p -> sb.append("<li><b>").append(p.getName()).append("</b> — ")
                .append(p.getQuantity()).append(" left (reorder at ").append(p.getReorderLevel()).append(")</li>"));
        return sb.append("</ul>").toString();
    }

    private String dead() {
        List<ProductMovement> dead = aiService.movementAnalysis().stream()
                .filter(m -> "DEAD".equals(m.getCategory())).limit(10).toList();
        if (dead.isEmpty()) return "✅ No dead stock — every product has sold recently.";
        StringBuilder sb = new StringBuilder("🪦 <b>Dead stock</b> (not selling):<ul class='mb-0'>");
        dead.forEach(m -> sb.append("<li><b>").append(m.getProductName()).append("</b> — ")
                .append(m.getDaysSinceLastSale() == null ? "never sold" : m.getDaysSinceLastSale() + " days idle")
                .append("</li>"));
        return sb.append("</ul>").toString();
    }

    private String fast() {
        List<ProductMovement> fast = aiService.movementAnalysis().stream()
                .filter(m -> "FAST".equals(m.getCategory()))
                .sorted(Comparator.comparingLong(ProductMovement::getTotalSold).reversed())
                .limit(8).toList();
        if (fast.isEmpty()) return "I don't have enough sales data to find fast movers yet.";
        StringBuilder sb = new StringBuilder("🔥 <b>Fast-moving / best-selling products</b>:<ul class='mb-0'>");
        fast.forEach(m -> sb.append("<li><b>").append(m.getProductName()).append("</b> — ")
                .append(m.getTotalSold()).append(" units sold</li>"));
        return sb.append("</ul>").toString();
    }

    private String slow() {
        List<ProductMovement> slow = aiService.movementAnalysis().stream()
                .filter(m -> "SLOW".equals(m.getCategory()))
                .sorted(Comparator.comparingLong(ProductMovement::getTotalSold))
                .limit(8).toList();
        if (slow.isEmpty()) return "No slow-moving products to report.";
        StringBuilder sb = new StringBuilder("🐢 <b>Slow-moving products</b>:<ul class='mb-0'>");
        slow.forEach(m -> sb.append("<li><b>").append(m.getProductName()).append("</b> — ")
                .append(m.getTotalSold()).append(" units sold</li>"));
        return sb.append("</ul>").toString();
    }

    private String expiring() {
        List<Product> exp = productRepository.findExpiringBefore(LocalDate.now().plusDays(expiryWarningDays));
        if (exp.isEmpty()) return "✅ No products are expiring within the next " + expiryWarningDays + " days.";
        StringBuilder sb = new StringBuilder("⏳ <b>Expiring soon</b>:<ul class='mb-0'>");
        exp.stream().limit(10).forEach(p -> sb.append("<li><b>").append(p.getName()).append("</b> — expires ")
                .append(p.getExpiryDate()).append("</li>"));
        return sb.append("</ul>").toString();
    }

    private String today() {
        DashboardStats s = reportService.dashboardStats();
        return "💰 <b>Today</b>: " + money(s.getTodaysRevenue()) + " in revenue from "
                + s.getTodaysTransactions() + " transaction(s).";
    }

    private String month() {
        DashboardStats s = reportService.dashboardStats();
        return "📅 <b>This month</b>: " + money(s.getMonthRevenue()) + " in total revenue so far.";
    }

    private String trend() {
        TrendAnalysis t = aiService.trendAnalysis(30, 7);
        if (t.getInsights().isEmpty()) return "Not enough data to analyse the sales trend yet.";
        StringBuilder sb = new StringBuilder("📈 <b>Sales trend</b>:<ul class='mb-0'>");
        t.getInsights().forEach(i -> sb.append("<li>").append(i).append("</li>"));
        return sb.append("</ul>").toString();
    }

    private String productCount() {
        long total = productService.countProducts();
        long low = productService.lowStock().size();
        return "📦 You have <b>" + total + "</b> products in the catalogue, of which <b>"
                + low + "</b> are low on stock.";
    }

    private String alerts() {
        List<Alert> alerts = aiService.alerts();
        if (alerts.isEmpty()) return "✅ No active alerts. Everything looks healthy.";
        StringBuilder sb = new StringBuilder("🔔 <b>Active alerts</b>:<ul class='mb-0'>");
        alerts.stream().limit(10).forEach(a -> sb.append("<li><b>").append(a.getTitle()).append(":</b> ")
                .append(a.getMessage()).append("</li>"));
        return sb.append("</ul>").toString();
    }

    private String wastage() {
        java.math.BigDecimal month = wastageService.monthLoss();
        List<WastagePrediction> pred = aiService.predictedWastage();
        StringBuilder sb = new StringBuilder("🗑️ <b>Wastage</b>: " + money(month) + " lost this month.");
        if (pred.isEmpty()) {
            sb.append(" No upcoming expiry wastage predicted. 👍");
        } else {
            sb.append("<br>Predicted upcoming wastage:<ul class='mb-0'>");
            pred.stream().limit(5).forEach(w -> sb.append("<li>").append(w.getMessage()).append("</li>"));
            sb.append("</ul>");
        }
        return sb.toString();
    }

    private String fallback() {
        return "🤔 I'm not sure about that one. " + help();
    }

    // ---- helpers ------------------------------------------------------------

    private boolean contains(String q, String... keys) {
        for (String k : keys) if (q.contains(k)) return true;
        return false;
    }

    private String money(BigDecimal v) {
        return currency + " " + String.format("%,.2f", v == null ? BigDecimal.ZERO : v);
    }
}
