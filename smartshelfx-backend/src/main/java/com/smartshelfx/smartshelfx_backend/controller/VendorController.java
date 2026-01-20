package com.smartshelfx.smartshelfx_backend.controller;

import com.smartshelfx.smartshelfx_backend.model.*;
import com.smartshelfx.smartshelfx_backend.Repository.*;
import com.smartshelfx.smartshelfx_backend.dto.SaleItemDTO;
import com.smartshelfx.smartshelfx_backend.dto.SalesReportDTO;
import com.smartshelfx.smartshelfx_backend.dto.StatusUpdateRequest; // Import from your dto package
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vendor")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*")
public class VendorController {
    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private StockTransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/sales-report")
    public ResponseEntity<SalesReportDTO> getVendorSalesReport() {
        // 1. Get logged-in vendor's email (username) from Security Context
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // 2. Find vendor from database using email
        User vendor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        // 3. Fetch 'OUT' transactions for this vendor
        List<StockTransaction> transactions = transactionRepository.findSalesByVendorId(vendor.getId());

        // 4. Map transactions to SaleItemDTOs
        List<SaleItemDTO> items = transactions.stream().map(t -> {
            SaleItemDTO item = new SaleItemDTO();
            item.setProductName(t.getProduct().getName());
            item.setQuantitySold(t.getQuantity());
            item.setPriceAtSale(t.getProduct().getPrice());
            item.setTotalItemRevenue(t.getQuantity() * t.getProduct().getPrice());
            item.setSaleDate(t.getTimestamp());
            return item;
        }).collect(Collectors.toList());

        // 5. Calculate Total Net Revenue
        Double totalRevenue = items.stream()
                .mapToDouble(SaleItemDTO::getTotalItemRevenue)
                .sum();

        // 6. Return the packaged report
        return ResponseEntity.ok(new SalesReportDTO(items, totalRevenue));
    }

    private final ProductRepository productRepository;
    // private final UserRepository userRepository;
    private final StockRequestRepository stockRequestRepository;
    private final WarehouseStockRepository warehouseStockRepo;
    private final StockTransactionRepository stockTransactionRepo; // Added for audit logging

    private User getAuthenticatedVendor() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
    }

    /**
     * FEATURE: Approve or Reject a Reorder Request
     * Logic: Updates Request Status, Global Product Stock, Warehouse Stock, and creates a Transaction Log.
     */
    @PostMapping("/reorders/{requestId}/status")
    @Transactional
    public ResponseEntity<?> updateRequestStatus(
            @PathVariable Long requestId, 
            @RequestBody StatusUpdateRequest statusUpdate) {
        
        User vendor = getAuthenticatedVendor();
        StockRequest request = stockRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Stock Request not found"));

        // Debug log to verify role
        System.out.println("User Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());

        // SECURITY: Verify ownership
        if (!request.getProduct().getVendor().getId().equals(vendor.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "Access Denied: Product ownership mismatch."));
        }

        // 1. Handle Rejection
        if ("REJECTED".equalsIgnoreCase(statusUpdate.getStatus())) {
            request.setStatus("REJECTED");
            stockRequestRepository.save(request);
            return ResponseEntity.ok(java.util.Map.of("message", "Request rejected."));
        }

        // 2. Handle Approval
        if ("APPROVED".equalsIgnoreCase(statusUpdate.getStatus())) {
            if (!"PENDING".equals(request.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("error", "Request is already processed."));
            }

            request.setStatus("APPROVED");

            // A. Update Global Product Stock
            Product product = request.getProduct();
            product.setCurrentStock(product.getCurrentStock() + request.getRequestedQuantity());
            productRepository.save(product);

            // B. Update Warehouse Specific Stock
            WarehouseStock warehouseStock = warehouseStockRepo
                    .findByWarehouseIdAndProductId(request.getWarehouseId(), product.getId())
                    .orElseThrow(() -> new RuntimeException("Warehouse stock record not found."));

            // Ensure we handle potential nulls safely
            int currentWarehouseQty = (warehouseStock.getCurrentStock() != null) ? warehouseStock.getCurrentStock() : 0;
            int requestedQty = (request.getRequestedQuantity() != null) ? request.getRequestedQuantity() : 0;

            // Set the new total
            warehouseStock.setCurrentStock(currentWarehouseQty + requestedQty);
            warehouseStockRepo.save(warehouseStock);

            // C. Create Audit Log (Transaction)
            StockTransaction tx = StockTransaction.builder()
                    .product(product)
                    .quantity(request.getRequestedQuantity())
                    .type("IN")
                    .reason("Vendor Fulfillment - Request #" + requestId)
                    .warehouseId(request.getWarehouseId())
                    .handledBy(vendor)
                    .timestamp(LocalDateTime.now())
                    .build();
            stockTransactionRepo.save(tx);

            stockRequestRepository.save(request);
            
            
            // Return JSON object so Angular doesn't throw a parsing error
            return ResponseEntity.ok(java.util.Map.of(
                "message", "Request approved. Inventory synced and logged.",
                "newStock", product.getCurrentStock()
            ));
        }

        return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid status provided."));
    }

    @GetMapping("/my-products")
    public ResponseEntity<List<Product>> getMyProducts() {
        User vendor = getAuthenticatedVendor();
        return ResponseEntity.ok(productRepository.findByVendorAndDeletedFalse(vendor));
    }

    @GetMapping("/reorders")
    public ResponseEntity<List<StockRequest>> getVendorReorders() {
        User currentVendor = getAuthenticatedVendor();
        List<StockRequest> requests = stockRequestRepository.findByProductVendor(currentVendor);
        return ResponseEntity.ok(requests);
    }

    // @PostMapping("/reorders/create")
    // public ResponseEntity<StockRequest> createReorderRequest(@RequestBody StockRequest request) {
    //     // Ensure the request starts as PENDING
    //     request.setStatus("PENDING");
    //     request.setRequestDate(LocalDateTime.now());
        
    //     // The product ID sent from Angular will be automatically mapped to the Product object
    //     StockRequest savedRequest = stockRequestRepository.save(request);
    //     return ResponseEntity.status(HttpStatus.CREATED).body(savedRequest);
    // }

    @PostMapping("/reorders")
public ResponseEntity<?> createReorderRequest(@RequestBody StockRequest request) {
    try {
        // 1. Get the logged-in user (The Manager)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User manager = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        // 2. Set the required fields
        request.setManagerId(manager.getId()); // Fixes the null managerId error
        request.setStatus("PENDING");
        request.setRequestDate(LocalDateTime.now());

        // 3. Save to database
        StockRequest saved = stockRequestRepository.save(request);
        return ResponseEntity.ok(saved);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
    }
}

    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProductDetails(@PathVariable Long id, @RequestBody Product updatedDetails) {
        User vendor = getAuthenticatedVendor();
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!existingProduct.getVendor().getId().equals(vendor.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        existingProduct.setName(updatedDetails.getName());
        existingProduct.setDescription(updatedDetails.getDescription());
        existingProduct.setPrice(updatedDetails.getPrice());
        existingProduct.setImageUrl(updatedDetails.getImageUrl());

        return ResponseEntity.ok(productRepository.save(existingProduct));
    }

    @PostMapping("/products/upload-csv")
    public ResponseEntity<?> uploadProductCSV(@RequestParam("file") MultipartFile file) {
        User vendor = getAuthenticatedVendor();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            int updatedCount = 0;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) { isFirstLine = false; continue; }
                String[] data = line.split(",");
                if (data.length < 4) continue; 
                String sku = data[0].trim();
                Product product = productRepository.findBySku(sku).orElse(null);
                if (product != null && product.getVendor().getId().equals(vendor.getId())) {
                    product.setPrice(Double.parseDouble(data[1].trim()));
                    product.setDescription(data[2].trim());
                    product.setImageUrl(data[3].trim());
                    productRepository.save(product);
                    updatedCount++;
                }
            }
            return ResponseEntity.ok("Successfully updated " + updatedCount + " products.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/notifications/low-stock")
    public ResponseEntity<List<Product>> getLowStockNotifications() {
        User vendor = getAuthenticatedVendor();
        List<Product> lowStockProducts = productRepository.findAll().stream()
                .filter(p -> p.getVendor().getId().equals(vendor.getId()))
                .filter(p -> p.getCurrentStock() <= p.getReorderLevel())
                .filter(p -> !p.isDeleted())
                .toList();
        return ResponseEntity.ok(lowStockProducts);
    }
    // @GetMapping("/sales-report")
    // public ResponseEntity<?> getVendorSales(Principal principal) {
    //     String vendorUsername = principal.getName();
        
    //     // Use the new repository to find sales
    //     List<OrderItem> sales = orderItemRepository.findByProduct_Vendor_Email(vendorUsername);
        
    //     double totalRevenue = sales.stream()
    //         .mapToDouble(item -> item.getPriceAtSale() * item.getQuantitySold())
    //         .sum();

    //     Map<String, Object> response = new HashMap<>();
    //     response.put("items", sales);
    //     response.put("totalRevenue", totalRevenue);
        
    //     return ResponseEntity.ok(response);
    // }
    
}