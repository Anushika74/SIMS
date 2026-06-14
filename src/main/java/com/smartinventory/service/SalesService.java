package com.smartinventory.service;

import com.smartinventory.dto.SaleItemRequest;
import com.smartinventory.dto.SaleRequest;
import com.smartinventory.model.Product;
import com.smartinventory.model.Sale;
import com.smartinventory.model.SaleItem;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/** Records sales, decrements stock and exposes sales history / invoices. */
@Service
public class SalesService {

    private static final DateTimeFormatter INV_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public SalesService(SaleRepository saleRepository, ProductRepository productRepository,
                        InventoryService inventoryService) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Records a sale: validates available stock, builds the invoice, decrements
     * stock and persists everything in a single transaction.
     */
    @Transactional
    public Sale recordSale(SaleRequest request, String cashier) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("A sale must contain at least one item.");
        }

        Sale sale = new Sale();
        sale.setInvoiceNo(generateInvoiceNo());
        sale.setSaleDate(LocalDateTime.now());
        sale.setCashier(cashier);
        sale.setPaymentMethod(request.getPaymentMethod() == null ? "CASH" : request.getPaymentMethod());

        BigDecimal total = BigDecimal.ZERO;

        for (SaleItemRequest line : request.getItems()) {
            if (line.getProductId() == null || line.getQuantity() == null || line.getQuantity() <= 0) {
                continue;
            }
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + line.getProductId()));

            if (product.getQuantity() < line.getQuantity()) {
                throw new IllegalArgumentException("Not enough stock for " + product.getName()
                        + " (available: " + product.getQuantity() + ", requested: " + line.getQuantity() + ")");
            }

            SaleItem item = new SaleItem(product, line.getQuantity(), product.getUnitPrice());
            sale.addItem(item);
            total = total.add(item.getLineTotal());

            inventoryService.reduceStockForSale(product, line.getQuantity(), cashier);
        }

        if (sale.getItems().isEmpty()) {
            throw new IllegalArgumentException("No valid items in the sale.");
        }

        sale.setTotalAmount(total);
        return saleRepository.save(sale);
    }

    public Optional<Sale> findById(Long id) {
        return saleRepository.findById(id);
    }

    public List<Sale> recentSales() {
        return saleRepository.findTop10ByOrderBySaleDateDesc();
    }

    public List<Sale> salesBetween(LocalDateTime from, LocalDateTime to) {
        return saleRepository.findBySaleDateBetweenOrderBySaleDateDesc(from, to);
    }

    private String generateInvoiceNo() {
        return "INV-" + LocalDateTime.now().format(INV_FMT) + "-"
                + ThreadLocalRandom.current().nextInt(100, 999);
    }
}
