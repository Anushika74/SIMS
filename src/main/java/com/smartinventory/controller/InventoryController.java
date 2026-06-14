package com.smartinventory.controller;

import com.smartinventory.service.InventoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String view(Model model) {
        model.addAttribute("products", inventoryService.availableStock());
        model.addAttribute("lowStock", inventoryService.lowStock());
        model.addAttribute("movements", inventoryService.recentMovements());
        model.addAttribute("active", "inventory");
        return "inventory";
    }

    @PostMapping("/add")
    public String addStock(@RequestParam Long productId,
                           @RequestParam int quantity,
                           @RequestParam(required = false) String reason,
                           Principal principal) {
        inventoryService.addStock(productId, quantity, reason, principal.getName());
        return "redirect:/inventory";
    }

    @PostMapping("/adjust")
    public String adjustStock(@RequestParam Long productId,
                              @RequestParam int quantity,
                              @RequestParam(required = false) String reason,
                              Principal principal) {
        inventoryService.setStock(productId, quantity, reason, principal.getName());
        return "redirect:/inventory";
    }
}
