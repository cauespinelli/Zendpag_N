package com.zendapag.api.controller;

import com.zendapag.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "Analytics", description = "Analytics and Dashboard APIs")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    @Operation(summary = "Get dashboard statistics")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats(
            @RequestParam(required = false, defaultValue = "30days") String period) {
        
        Map<String, Object> stats = new LinkedHashMap<>();
        
        // Summary stats
        stats.put("totalUsers", 27);
        stats.put("documentsInAnalysis", 6);
        stats.put("totalWithdrawals", 9698);
        stats.put("withdrawalsAmount", new BigDecimal("764599.01"));
        stats.put("totalProfit", new BigDecimal("10745.56"));
        
        // Payment methods breakdown
        List<Map<String, Object>> paymentMethods = new ArrayList<>();
        paymentMethods.add(Map.of(
            "name", "PIX",
            "value", new BigDecimal("774675.02"),
            "percent", 68,
            "color", "#00C853"
        ));
        paymentMethods.add(Map.of(
            "name", "Cartão",
            "value", BigDecimal.ZERO,
            "percent", 22,
            "color", "#4A90D9"
        ));
        paymentMethods.add(Map.of(
            "name", "Boleto",
            "value", BigDecimal.ZERO,
            "percent", 10,
            "color", "#C9A962"
        ));
        stats.put("paymentMethods", paymentMethods);
        
        // Chart data (last 30 days)
        List<Map<String, Object>> chartData = new ArrayList<>();
        chartData.add(Map.of("name", "01", "value", 45000));
        chartData.add(Map.of("name", "05", "value", 52000));
        chartData.add(Map.of("name", "10", "value", 38000));
        chartData.add(Map.of("name", "15", "value", 65000));
        chartData.add(Map.of("name", "20", "value", 78000));
        chartData.add(Map.of("name", "25", "value", 55000));
        chartData.add(Map.of("name", "30", "value", 82000));
        stats.put("chartData", chartData);
        
        // Today stats
        Map<String, Object> todayStats = new LinkedHashMap<>();
        todayStats.put("pixToday", new BigDecimal("32150.00"));
        todayStats.put("cardToday", new BigDecimal("10288.00"));
        todayStats.put("boletoToday", new BigDecimal("4822.50"));
        todayStats.put("transactionsToday", 87);
        stats.put("todayStats", todayStats);
        
        // Totals
        stats.put("totalAmount", new BigDecimal("774675.02"));
        stats.put("netAmount", new BigDecimal("774176.82"));
        stats.put("pendingWithdrawals", 2);
        
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved", stats));
    }

    @Operation(summary = "Get transaction analytics")
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTransactionAnalytics(
            @RequestParam(required = false, defaultValue = "30days") String period,
            @RequestParam(required = false, defaultValue = "day") String groupBy) {
        
        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("period", period);
        analytics.put("groupBy", groupBy);
        analytics.put("totalTransactions", 1250);
        analytics.put("successfulTransactions", 1180);
        analytics.put("failedTransactions", 70);
        analytics.put("successRate", 94.4);
        
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            data.add(Map.of(
                "date", LocalDate.now().minusDays(7 - i).toString(),
                "count", 150 + (int)(Math.random() * 50),
                "amount", new BigDecimal(String.valueOf(50000 + Math.random() * 30000))
            ));
        }
        analytics.put("data", data);
        
        return ResponseEntity.ok(ApiResponse.success("Transaction analytics retrieved", analytics));
    }

    @Operation(summary = "Get revenue analytics")
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenueAnalytics(
            @RequestParam(required = false, defaultValue = "30days") String period) {
        
        Map<String, Object> revenue = new LinkedHashMap<>();
        revenue.put("period", period);
        revenue.put("totalRevenue", new BigDecimal("774675.02"));
        revenue.put("netRevenue", new BigDecimal("764929.46"));
        revenue.put("fees", new BigDecimal("9745.56"));
        revenue.put("growth", 12.5);
        
        List<Map<String, Object>> breakdown = new ArrayList<>();
        breakdown.add(Map.of("source", "PIX", "amount", new BigDecimal("774675.02"), "percent", 100));
        revenue.put("breakdown", breakdown);
        
        return ResponseEntity.ok(ApiResponse.success("Revenue analytics retrieved", revenue));
    }
}
