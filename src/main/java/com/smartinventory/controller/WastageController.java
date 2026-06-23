package com.smartinventory.controller;

import com.smartinventory.dto.WastageRequest;
import com.smartinventory.model.Wastage;
import com.smartinventory.service.ProductService;
import com.smartinventory.service.WastageService;
import com.smartinventory.service.ai.AiService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Wastage Management — record losses (expired/damaged/etc.), view wastage
 * reports and AI predicted wastage.
 */
@Controller
@RequestMapping("/wastage")
public class WastageController {

    private final WastageService wastageService;
    private final ProductService productService;
    private final AiService aiService;

    public WastageController(WastageService wastageService, ProductService productService, AiService aiService) {
        this.wastageService = wastageService;
        this.productService = productService;
        this.aiService = aiService;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("products", productService.findAll());
        model.addAttribute("reasons", Wastage.Reason.values());
        model.addAttribute("recent", wastageService.recent());
        model.addAttribute("totalLoss", wastageService.totalLoss());
        model.addAttribute("monthLoss", wastageService.monthLoss());
        model.addAttribute("wastageCount", wastageService.count());
        model.addAttribute("byReason", wastageService.byReason());
        model.addAttribute("topProducts", wastageService.topWastedProducts());
        model.addAttribute("predicted", aiService.predictedWastage());
        model.addAttribute("active", "wastage");
        return "wastage";
    }

    @PostMapping("/record")
    public String record(@ModelAttribute WastageRequest request, Principal principal, RedirectAttributes ra) {
        try {
            Wastage w = wastageService.record(request, principal.getName());
            ra.addFlashAttribute("message", "Recorded wastage of " + w.getQuantity() + " x "
                    + w.getProduct().getName() + " (loss " + w.getLossAmount() + ").");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/wastage";
    }
}
