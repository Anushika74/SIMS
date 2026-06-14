package com.smartinventory.controller;

import com.smartinventory.dto.ProductMovement;
import com.smartinventory.service.ai.AiService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * AI Insights Dashboard + Prediction & Analytics pages (the two AI screens).
 */
@Controller
@RequestMapping("/ai")
public class AiInsightsController {

    private final AiService aiService;

    public AiInsightsController(AiService aiService) {
        this.aiService = aiService;
    }

    /** AI Insights Dashboard — alerts, movement analysis, restock action items. */
    @GetMapping("/insights")
    public String insights(Model model) {
        List<ProductMovement> movements = aiService.movementAnalysis();
        model.addAttribute("alerts", aiService.alerts());
        model.addAttribute("movementSummary", aiService.movementSummary());
        model.addAttribute("fastMovers", movements.stream()
                .filter(m -> "FAST".equals(m.getCategory())).limit(10).toList());
        model.addAttribute("deadStock", movements.stream()
                .filter(m -> "DEAD".equals(m.getCategory())).limit(10).toList());
        model.addAttribute("restock", aiService.restockActionItems());
        model.addAttribute("mlAvailable", aiService.isMlServiceAvailable());
        model.addAttribute("active", "ai-insights");
        return "ai-insights";
    }

    /** Prediction & Analytics — sales-trend forecast + restock prediction table. */
    @GetMapping("/analytics")
    public String analytics(@RequestParam(value = "days", defaultValue = "30") int days,
                            @RequestParam(value = "forecast", defaultValue = "7") int forecast,
                            Model model) {
        model.addAttribute("trend", aiService.trendAnalysis(days, forecast));
        model.addAttribute("restock", aiService.restockPredictions());
        model.addAttribute("movements", aiService.movementAnalysis());
        model.addAttribute("mlAvailable", aiService.isMlServiceAvailable());
        model.addAttribute("days", days);
        model.addAttribute("forecast", forecast);
        model.addAttribute("active", "ai-analytics");
        return "ai-analytics";
    }
}
