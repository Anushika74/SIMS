package com.smartinventory.controller;

import com.smartinventory.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String reports(Model model) {
        model.addAttribute("dailySales", reportService.dailySales(30));
        model.addAttribute("monthlySales", reportService.monthlySales());
        model.addAttribute("products", reportService.productStockReport());
        model.addAttribute("active", "reports");
        return "reports";
    }
}
