package com.smartshelfx.smartshelfx_backend.controller;

import com.smartshelfx.smartshelfx_backend.model.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import com.smartshelfx.smartshelfx_backend.Repository.*;
import com.smartshelfx.smartshelfx_backend.Service.ReportService;
import com.smartshelfx.smartshelfx_backend.Service.VendorService;
import com.smartshelfx.smartshelfx_backend.dto.InventoryTrendDTO;
import com.smartshelfx.smartshelfx_backend.dto.MonthlyComparisonDTO;
import com.smartshelfx.smartshelfx_backend.dto.ProductImportDTO;
import com.smartshelfx.smartshelfx_backend.dto.VendorPerformanceDTO;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    private final ProductRepository productRepository;
    private final StockTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WarehouseStockRepository warehouseStockRepo; 
    private final StockRequestRepository stockRequestRepo;
    private  final VendorService vendorService;
    private final ReportService reportService;

    // Add this endpoint for your Admin Dashboard Reports Tab
    @GetMapping("/inventory-trends")
    public ResponseEntity<List<InventoryTrendDTO>> getAdminInventoryTrends() {
        // Reuse the logic already written in ReportService
        return ResponseEntity.ok(reportService.getInventoryTrends());
    }
    
    @GetMapping("/users")
    public List<User> getAllVendors() {
        return userRepository.findAllByRole("VENDOR");
    }

    @GetMapping("/users-all")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody String newRole) {
        return userRepository.findById(id).map(user -> {
            user.setRole(newRole.replace("\"", "")); 
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Role updated successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/assign-warehouse/{userId}/{warehouseId}")
    @Transactional
    public ResponseEntity<?> assignWarehouse(@PathVariable Long userId, @PathVariable Long warehouseId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setWarehouseId(warehouseId);
            userRepository.save(user);
            
            List<Product> allProducts = productRepository.findByDeletedFalse();
            for (Product p : allProducts) {
                warehouseStockRepo.findByWarehouseIdAndProductId(warehouseId, p.getId())
                    .ifPresentOrElse(ws -> {}, () -> {
                        WarehouseStock newStock = WarehouseStock.builder()
                            .warehouseId(warehouseId)
                            .product(p)
                            .currentStock(0) 
                            .reorderLevel(10)
                            .build();
                        warehouseStockRepo.save(newStock);
                    });
            }
            return ResponseEntity.ok(Map.of("message", "Manager assigned to warehouse " + warehouseId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }


    @GetMapping("/products")
    public List<Product> getAllProducts() {
        return productRepository.findByDeletedFalse();
    }

    @PostMapping("/products")
    @Transactional
    public ResponseEntity<?> addProduct(@RequestBody Product product) {
        try {
            if (product.getVendor() != null && product.getVendor().getId() != null) {
                User existingVendor = userRepository.findById(product.getVendor().getId())
                    .orElseThrow(() -> new RuntimeException("Vendor not found"));
                
                // CRITICAL FIX: Explicitly set the numeric vendorId field
                product.setVendorId(existingVendor.getId());
                product.setVendor(existingVendor);
            } else {
                return ResponseEntity.badRequest().body("Vendor selection is required");
            }

            Product savedProduct = productRepository.save(product);

            // Initialize warehouses
            List<Long> warehouseIds = userRepository.findAll().stream()
                .map(User::getWarehouseId)
                .filter(id -> id != null && id != 0)
                .distinct()
                    .collect(Collectors.toList());

            if (warehouseIds.isEmpty()) warehouseIds.add(1L);

            for (Long whId : warehouseIds) {
                WarehouseStock stock = WarehouseStock.builder()
                    .warehouseId(whId)
                    .product(savedProduct)
                    .currentStock(whId == 1L ? savedProduct.getCurrentStock() : 0)
                    .reorderLevel(10)
                    .build();
                warehouseStockRepo.save(stock);
            }
                    
            logTransaction(savedProduct, savedProduct.getCurrentStock(), "IN", "Initial Stock Entry via Admin");
            return ResponseEntity.ok(savedProduct);
            
        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/products/{id}")
    @Transactional
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product details) {
        try {
            Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

            int diff = details.getCurrentStock() - product.getCurrentStock();
            
            product.setName(details.getName());
            product.setSku(details.getSku());
            product.setCategory(details.getCategory());
            product.setPrice(details.getPrice());
            product.setCurrentStock(details.getCurrentStock());
            product.setReorderLevel(details.getReorderLevel());


            if (details.getVendor() != null && details.getVendor().getId() != null) {
                User v = userRepository.findById(details.getVendor().getId()).orElse(null);
                if (v != null) {
                    product.setVendor(v);
                    product.setVendorId(v.getId());
                }
            }

            Product updated = productRepository.save(product);


            warehouseStockRepo.findByWarehouseIdAndProductId(1L, updated.getId())
                .ifPresent(ws -> {
                    ws.setCurrentStock(updated.getCurrentStock());
                    ws.setReorderLevel(updated.getReorderLevel());
                    warehouseStockRepo.save(ws);
                });

            String type = (diff > 0) ? "ADJUST_IN" : (diff < 0 ? "ADJUST_OUT" : "UPDATE");
            logTransaction(updated, Math.abs(diff), type, "Manual Adjustment/Update");

            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Update failed: " + e.getMessage());
        }
    }
    @DeleteMapping("/products/{id}")
@Transactional 
public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
    try {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        String originalSku = product.getSku();
        

        logTransaction(product, 0, "DELETE", "Archived product. Original SKU: " + originalSku);
        

        product.setSku(originalSku + "_DEL_" + System.currentTimeMillis());
        product.setDeleted(true);
        productRepository.save(product);


        warehouseStockRepo.deleteByProductId(id);

        return ResponseEntity.ok(Map.of(
            "message", "Product archived successfully",
            "archivedSku", product.getSku()
        ));
        
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", e.getMessage()));
    }
}

    @PostMapping("/products/import")
public ResponseEntity<Map<String, Object>> importProducts(@RequestParam("file") MultipartFile file) {
    Map<String, Object> response = new HashMap<>();
    List<String> duplicates = new ArrayList<>();
    int count = 0;
    
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
        String line;
        boolean isHeader = true;
        
        while ((line = reader.readLine()) != null) {
            if (isHeader) { isHeader = false; continue; } 
            
            String[] data = line.split(",");
            
            Product product = new Product();
            String sku = data[0].trim().replace("\"", "");
            product.setSku(sku);

            if (productRepository.existsBySku(sku)) {
                duplicates.add(sku);
                continue; 
            }
            product.setName(data[1].trim().replace("\"", ""));
            product.setCategory(data[2].trim().replace("\"", ""));
            try {
                Long vId = Long.parseLong(data[3].trim().replace("\"", ""));
                product.setVendorId(userRepository.findById(vId).map(User::getId).orElse(null));
            } catch (Exception e) {
                product.setVendorId(null);
            }

            product.setCurrentStock(0);
            product.setReorderLevel(10);
            product.setPrice(Double.parseDouble(data[4].trim()));
            
            Product savedProduct = productRepository.save(product);
            List<Long> warehouseIds = userRepository.findAll().stream()
                .map(User::getWarehouseId)
                .filter(id -> id != null && id != 0)
                .distinct()
                    .collect(Collectors.toList());

            if (warehouseIds.isEmpty()) warehouseIds.add(1L);

            for (Long whId : warehouseIds) {
                WarehouseStock stock = WarehouseStock.builder()
                    .warehouseId(whId)
                    .product(savedProduct)
                    .currentStock(whId == 1L ? savedProduct.getCurrentStock() : 0)
                    .reorderLevel(10)
                    .build();
                warehouseStockRepo.save(stock);
            }
            count++;
        }
        String msg = "Bulk products added successfully: " + count;
        if (!duplicates.isEmpty()) {
            msg += ". These SKUs already exist: " + String.join(", ", duplicates);
        }

        response.put("status", "success");
        response.put("message",msg);
        return ResponseEntity.ok(response);

    } catch (Exception e) {
        response.put("status", "error");
        response.put("message", "Error: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}



    @GetMapping("/overall-health")
    public ResponseEntity<?> getOverallHealth() {
        List<Product> products = productRepository.findByDeletedFalse();
        Map<String, Object> stats = new HashMap<>();
        
        long totalStock = products.stream().mapToLong(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0).sum();
        long lowStockCount = products.stream().filter(p -> p.getCurrentStock() <= p.getReorderLevel()).count();
        
        stats.put("totalInventory", totalStock);
        stats.put("lowStockItems", lowStockCount);
        stats.put("totalProducts", products.size());
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/audit-logs")
    public List<StockTransaction> getAuditLogs() {
        return transactionRepository.findAllByOrderByTimestampDesc();
    }

    @GetMapping("/low-stock-alerts")
    public List<Product> getLowStockAlerts() {
        return productRepository.findLowStockProducts();
    }

    // --- REORDER REQUESTS ---

    @GetMapping("/reorder-requests")
    public ResponseEntity<List<StockRequest>> getAllReorderRequests() {
        return ResponseEntity.ok(stockRequestRepo.findAll());
    }

    @PostMapping("/reorder-requests/{id}/status")
    @Transactional
    public ResponseEntity<?> updateRequestStatus(@PathVariable Long id, @RequestParam String status) {
        StockRequest request = stockRequestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if ("APPROVED".equalsIgnoreCase(status)) {
            Product product = request.getProduct();
            // 1. Update Global Stock
            product.setCurrentStock(product.getCurrentStock() + request.getRequestedQuantity());
            productRepository.save(product);
            
            // 2. Update Specific Warehouse Stock
            warehouseStockRepo.findByWarehouseIdAndProductId(request.getWarehouseId(), product.getId())
                .ifPresent(ws -> {
                    ws.setCurrentStock(ws.getCurrentStock() + request.getRequestedQuantity());
                    warehouseStockRepo.save(ws);
                });
        }

        request.setStatus(status.toUpperCase());
        stockRequestRepo.save(request);
        return ResponseEntity.ok(Map.of("message", "Request " + status));
    }

    // --- HELPERS ---

    private void logTransaction(Product product, Integer qty, String type, String reason) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        StockTransaction tx = new StockTransaction(); 
        tx.setProduct(product);
        tx.setQuantity(qty);
        tx.setType(type);
        tx.setReason(reason);
        tx.setTimestamp(LocalDateTime.now());
        tx.setHandledBy(userRepository.findByEmail(email).orElse(null));
        transactionRepository.save(tx);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getPredefinedCategories() {
        List<String> categories = Arrays.asList(
            "Stationery", 
            "Electronics", 
            "Furniture", 
            "Groceries"
        );
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/vendor-performance")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<VendorPerformanceDTO>> getPerformanceReport() {
        return ResponseEntity.ok(vendorService.getVendorPerformanceReport());
    }

    @GetMapping("/export-pdf")
    public void exportToPDF(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=SmartShelfX_Inventory.pdf";
        response.setHeader(headerKey, headerValue);
        
        reportService.generateInventoryPdf(response);
    }

        @GetMapping("/monthly-comparison")
        public ResponseEntity<List<MonthlyComparisonDTO>> getMonthlyComparison() {
            return ResponseEntity.ok(reportService.getMonthlyComparison());
        }
}