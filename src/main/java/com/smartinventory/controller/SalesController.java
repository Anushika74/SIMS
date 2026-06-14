package com.smartinventory.controller;

import com.smartinventory.dto.SaleItemRequest;
import com.smartinventory.dto.SaleRequest;
import com.smartinventory.model.Sale;
import com.smartinventory.service.ProductService;
import com.smartinventory.service.SalesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/sales")
public class SalesController {

    private final SalesService salesService;
    private final ProductService productService;

    public SalesController(SalesService salesService, ProductService productService) {
        this.salesService = salesService;
        this.productService = productService;
    }

    @GetMapping
    public String salesPage(Model model) {
        model.addAttribute("products", productService.findAll());
        model.addAttribute("recentSales", salesService.recentSales());
        model.addAttribute("active", "sales");
        return "sales";
    }

    @PostMapping("/record")
    public String recordSale(@RequestParam("productId") List<Long> productIds,
                             @RequestParam("quantity") List<Integer> quantities,
                             @RequestParam(value = "paymentMethod", defaultValue = "CASH") String paymentMethod,
                             Principal principal,
                             RedirectAttributes ra) {
        SaleRequest request = new SaleRequest();
        request.setPaymentMethod(paymentMethod);
        List<SaleItemRequest> items = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            Integer qty = i < quantities.size() ? quantities.get(i) : null;
            if (productIds.get(i) != null && qty != null && qty > 0) {
                SaleItemRequest item = new SaleItemRequest();
                item.setProductId(productIds.get(i));
                item.setQuantity(qty);
                items.add(item);
            }
        }
        request.setItems(items);

        try {
            Sale sale = salesService.recordSale(request, principal.getName());
            return "redirect:/sales/invoice/" + sale.getId();
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/sales";
        }
    }

    @GetMapping("/invoice/{id}")
    public String invoice(@PathVariable Long id, Model model) {
        Sale sale = salesService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + id));
        model.addAttribute("sale", sale);
        model.addAttribute("active", "sales");
        return "invoice";
    }

    @GetMapping("/history")
    public String history(@RequestParam(value = "from", required = false) String from,
                          @RequestParam(value = "to", required = false) String to,
                          Model model) {
        LocalDate fromDate = (from == null || from.isBlank()) ? LocalDate.now().minusDays(30) : LocalDate.parse(from);
        LocalDate toDate = (to == null || to.isBlank()) ? LocalDate.now() : LocalDate.parse(to);
        model.addAttribute("sales", salesService.salesBetween(
                fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX)));
        model.addAttribute("from", fromDate.toString());
        model.addAttribute("to", toDate.toString());
        model.addAttribute("active", "sales");
        return "sales-history";
    }
}
