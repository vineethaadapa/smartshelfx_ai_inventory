package com.smartshelfx.smartshelfx_backend.controller;

import com.smartshelfx.smartshelfx_backend.Service.ManagerInventoryService;
import com.smartshelfx.smartshelfx_backend.Service.StockService;
import com.smartshelfx.smartshelfx_backend.model.StockTransaction;
import com.smartshelfx.smartshelfx_backend.model.Product;
import com.smartshelfx.smartshelfx_backend.model.StockRequest;
import com.smartshelfx.smartshelfx_backend.model.User;
import com.smartshelfx.smartshelfx_backend.model.WarehouseStock;
import com.smartshelfx.smartshelfx_backend.Repository.UserRepository;
import com.smartshelfx.smartshelfx_backend.Repository.WarehouseStockRepository;
import lombok.RequiredArgsConstructor;
import com.smartshelfx.smartshelfx_backend.Service.StockService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class StockController {

    private final ManagerInventoryService inventoryService;
    private final UserRepository userRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final com.smartshelfx.smartshelfx_backend.Repository.ProductRepository productRepository;
    private final com.smartshelfx.smartshelfx_backend.Repository.StockRequestRepository stockRequestRepository;
    @Autowired
    private StockService stockService;



    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

  
    @GetMapping("/inventory")
    public ResponseEntity<?> getMyWarehouseInventory() {
        User manager = getAuthenticatedUser();
        // Uses the service to get WarehouseStock items
        return ResponseEntity.ok(inventoryService.getInventoryForManager(manager.getWarehouseId()));
    }

  
    @GetMapping("/transactions")
    public ResponseEntity<List<StockTransaction>> getHistory() {
        User manager = getAuthenticatedUser();
        return ResponseEntity.ok(inventoryService.getStockLogs(manager.getWarehouseId()));
    }

  
    @PostMapping("/update")
    public ResponseEntity<?> updateStock(@RequestBody Map<String, Object> request) {
        User manager = getAuthenticatedUser();
        try {
            StockTransaction tx = inventoryService.updateStock(
                manager.getWarehouseId(),
                Long.valueOf(request.get("productId").toString()),
                Integer.valueOf(request.get("quantity").toString()),
                request.get("type").toString(),
                request.get("reason").toString(),
                manager.getId()
            );
            return ResponseEntity.ok(tx);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

   
    @PostMapping("/reorder")
    public ResponseEntity<?> createReorder(@RequestBody Map<String, Object> requestData) {
        User manager = getAuthenticatedUser();
        try {
            StockRequest req = inventoryService.createReorderRequest(
                Long.valueOf(requestData.get("productId").toString()),
                Integer.valueOf(requestData.get("quantity").toString()),
                manager.getWarehouseId(),
                manager.getId(),
                requestData.get("notes") != null ? requestData.get("notes").toString() : "Low stock auto-request"
            );
            return ResponseEntity.ok(req);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/reorder-history")
    public ResponseEntity<List<StockRequest>> getReorderHistory() {
        User manager = getAuthenticatedUser();
        return ResponseEntity.ok(inventoryService.getReorderRequests(manager.getWarehouseId()));
    }


    @PostMapping("/import-csv")
    public ResponseEntity<?> importStockCSV(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please select a CSV file to upload."));
        }

        try {
            inventoryService.processStockCsv(file);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Stock updated successfully from CSV"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

 
    @GetMapping("/manager/inventory/{warehouseId}")
    public ResponseEntity<List<WarehouseStock>> getManagerInventory(@PathVariable Long warehouseId) {
        List<WarehouseStock> stock = warehouseStockRepository.findByWarehouseId(warehouseId);
        return ResponseEntity.ok(stock);
    }

@PostMapping("/ai-reorder")
public ResponseEntity<?> createAiReorder(@RequestBody Map<String, Object> payload, Principal principal) {
    try {
        // 1. Get the current logged-in manager
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Find the product by name (sent from Angular)
        String productName = (String) payload.get("productName");
        Product product = productRepository.findByName(productName)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productName));

        // 3. Build the StockRequest using your @Builder
        Double demand = Double.valueOf(payload.get("quantity").toString());
        
        StockRequest request = StockRequest.builder()
                .product(product)
                .requestedQuantity((int) Math.ceil(demand))
                .status("PENDING")
                .warehouseId(user.getWarehouseId()) // Taken from the User entity
                .managerId(user.getId())            // Taken from the User entity
                .managerNotes("AI Suggested Replenishment")
                .build();

        stockRequestRepository.save(request);
        return ResponseEntity.ok(Collections.singletonMap("message", "Request created successfully"));
        
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
}