package com.smartinventory.controller;

import com.smartinventory.model.Product;
import com.smartinventory.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q, Model model) {
        model.addAttribute("products", productService.search(q));
        model.addAttribute("q", q);
        model.addAttribute("active", "products");
        return "products";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", productService.allCategories());
        model.addAttribute("active", "products");
        return "product-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Product product = productService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        model.addAttribute("product", product);
        model.addAttribute("categories", productService.allCategories());
        model.addAttribute("active", "products");
        return "product-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Product product,
                       @RequestParam(value = "categoryId", required = false) Long categoryId) {
        if (categoryId != null) {
            product.setCategory(productService.findCategory(categoryId));
        }
        productService.save(product);
        return "redirect:/products";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            productService.delete(id);
            ra.addFlashAttribute("message", "Product deleted successfully.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage() != null ? ex.getMessage()
                    : "This product cannot be deleted because it is used in sales records.");
        }
        return "redirect:/products";
    }

    /** Barcode/SKU lookup used by the scanner on the Sales page (returns JSON). */
    @GetMapping("/by-barcode")
    @ResponseBody
    public Map<String, Object> byBarcode(@RequestParam("code") String code) {
        Map<String, Object> result = new HashMap<>();
        productService.findBySku(code).ifPresentOrElse(p -> {
            result.put("found", true);
            result.put("id", p.getId());
            result.put("name", p.getName());
            result.put("sku", p.getSku());
            result.put("unitPrice", p.getUnitPrice());
            result.put("quantity", p.getQuantity());
        }, () -> result.put("found", false));
        return result;
    }
}
