package com.smartinventory.controller;

import com.smartinventory.dto.DashboardStats;
import com.smartinventory.service.InventoryService;
import com.smartinventory.service.ReportService;
import com.smartinventory.service.SalesService;
import com.smartinventory.service.ai.AiService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final ReportService reportService;
    private final SalesService salesService;
    private final InventoryService inventoryService;
    private final AiService aiService;

    public DashboardController(ReportService reportService, SalesService salesService,
                               InventoryService inventoryService, AiService aiService) {
        this.reportService = reportService;
        this.salesService = salesService;
        this.inventoryService = inventoryService;
        this.aiService = aiService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardStats stats = reportService.dashboardStats();
        var alerts = aiService.alerts();
        stats.setActiveAlerts(alerts.size());

        model.addAttribute("stats", stats);
        model.addAttribute("recentSales", salesService.recentSales());
        model.addAttribute("lowStock", inventoryService.lowStock());
        model.addAttribute("alerts", alerts.stream().limit(5).toList());
        model.addAttribute("topRestock", aiService.restockActionItems().stream().limit(5).toList());
        model.addAttribute("active", "dashboard");
        return "dashboard";
    }
}
