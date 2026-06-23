package com.smartinventory.service;

import com.smartinventory.dto.DailySalesProjection;
import com.smartinventory.dto.DashboardStats;
import com.smartinventory.dto.MonthlySalesProjection;
import com.smartinventory.model.Product;
import com.smartinventory.repository.ProductRepository;
import com.smartinventory.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Builds the reports and the dashboard summary figures. */
@Service
public class ReportService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final com.smartinventory.repository.WastageRepository wastageRepository;

    public ReportService(SaleRepository saleRepository, ProductRepository productRepository,
                         com.smartinventory.repository.WastageRepository wastageRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.wastageRepository = wastageRepository;
    }

    public List<DailySalesProjection> dailySales(int days) {
        LocalDateTime from = LocalDate.now().minusDays(days).atStartOfDay();
        return saleRepository.findDailySales(from);
    }

    public List<MonthlySalesProjection> monthlySales() {
        return saleRepository.findMonthlySales();
    }

    public List<Product> productStockReport() {
        return productRepository.findAll();
    }

    public DashboardStats dashboardStats() {
        DashboardStats stats = new DashboardStats();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        stats.setTotalProducts(productRepository.count());
        stats.setLowStockCount(productRepository.findLowStock().size());
        stats.setTodaysTransactions(saleRepository.countBySaleDateBetween(startOfToday, endOfToday));
        stats.setTodaysRevenue(saleRepository.sumRevenueBetween(startOfToday, endOfToday));
        stats.setMonthRevenue(saleRepository.sumRevenueBetween(startOfMonth, now));
        stats.setMonthWastage(wastageRepository.sumLossBetween(startOfMonth, now));
        stats.setTotalSales(saleRepository.count());

        return stats;
    }
}
