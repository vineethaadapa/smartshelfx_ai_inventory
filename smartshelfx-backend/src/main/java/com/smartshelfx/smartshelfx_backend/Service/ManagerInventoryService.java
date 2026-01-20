package com.smartshelfx.smartshelfx_backend.Service;

import com.smartshelfx.smartshelfx_backend.Repository.*;
import com.smartshelfx.smartshelfx_backend.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerInventoryService {
    private final WarehouseStockRepository warehouseStockRepository;
    private final StockService stockService;
    private final WarehouseStockRepository warehouseStockRepo;
    private final StockTransactionRepository stockTransactionRepo;
    private final ProductRepository productRepo; // Needed to sync Admin dashboard
    private final StockRequestRepository stockRequestRepo; // New: For Reorder Requests
    private final AlertService alertService;

    public List<WarehouseStock> getInventoryForManager(Long warehouseId) {
        return warehouseStockRepo.findByWarehouseId(warehouseId);
    }

    private final UserRepository userRepository;
    @Transactional
public StockTransaction updateStock(Long warehouseId, Long productId, int qty, String type, String reason, Long managerId) {

    // 1. Find the specific warehouse record
    WarehouseStock stock = warehouseStockRepo
            .findByWarehouseIdAndProductId(warehouseId, productId)
            .orElseThrow(() -> new RuntimeException("Stock record not found for Warehouse."));

    Product product = stock.getProduct(); 

    switch (type.toUpperCase()) {
        case "IN":
            stock.setCurrentStock(stock.getCurrentStock() + qty);
            break;
        case "OUT":
            if (stock.getCurrentStock() < qty) {
                throw new RuntimeException("Insufficient stock in warehouse!");
            }
            stock.setCurrentStock(stock.getCurrentStock() - qty);
            break;
        case "ADJUSTMENT":
            stock.setCurrentStock(qty);
            break;
        default:
            throw new IllegalArgumentException("Invalid transaction type: " + type);
    }

    warehouseStockRepo.save(stock);
    stockService.logTransaction(product, qty, type, reason, warehouseId);


    if (stock.getCurrentStock() <= stock.getReorderLevel()) {
        
    
        Long managerOfWarehouse = stock.getWarehouseId();

        alertService.createManagerNotification(
            managerOfWarehouse, // Use the actual manager ID
            "Low Stock Alert", 
            "Low stock in your warehouse for " + product.getName() + 
            ". Current stock: " + stock.getCurrentStock()
        );
    }

    return stockTransactionRepo.findTopByWarehouseIdOrderByTimestampDesc(warehouseId);
}

    @Transactional
    public StockRequest createReorderRequest(Long productId, Integer qty, Long warehouseId, Long managerId, String notes) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

        StockRequest request = StockRequest.builder()
                .product(product)
                .requestedQuantity(qty)
                .warehouseId(warehouseId)
                .managerId(managerId)
                .managerNotes(notes)
                .status("PENDING")
                .requestDate(LocalDateTime.now())
                .build();

        return stockRequestRepo.save(request);
    }

    public List<StockRequest> getReorderRequests(Long warehouseId) {
        return stockRequestRepo.findByWarehouseId(warehouseId);
    }

   
    public List<StockTransaction> getStockLogs(Long warehouseId) {
        return stockTransactionRepo.findByWarehouseId(warehouseId);
    }

  
    public List<WarehouseStock> getLowStock(Long warehouseId) {
        return warehouseStockRepo.findByWarehouseId(warehouseId)
                .stream()
                .filter(s -> s.getCurrentStock() <= s.getReorderLevel())
                .toList();
    }

    @Transactional
    public void processStockCsv(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            reader.readLine(); // Skip header
            
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length < 2) continue;

                String sku = data[0].trim();
                int newStockLevel = Integer.parseInt(data[1].trim());

                productRepo.findBySku(sku).ifPresent(product -> {
                    // 1. Update Global Product
                    product.setCurrentStock(newStockLevel);
                    productRepo.save(product);

                    // 2. Sync with Warehouse (Defaulting to Warehouse 1L)
                    warehouseStockRepo.findByWarehouseIdAndProductId(1L, product.getId())
                        .ifPresentOrElse(ws -> {
                            ws.setCurrentStock(newStockLevel);
                            warehouseStockRepo.save(ws);
                        }, () -> {
                            WarehouseStock newWs = WarehouseStock.builder()
                                    .warehouseId(1L)
                                    .product(product)
                                    .currentStock(newStockLevel)
                                    .reorderLevel(product.getReorderLevel())
                                    .build();
                            warehouseStockRepo.save(newWs);
                        });
                    
                    stockService.logTransaction(product, newStockLevel, "ADJUSTMENT", "Bulk update via CSV", 1L);
                    
                    // 3. Check for alerts after bulk update
                    alertService.generateLowStockAlert(product);
                });
            }
        }
    }

}