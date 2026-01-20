package com.smartshelfx.smartshelfx_backend.Service;

import com.smartshelfx.smartshelfx_backend.Repository.*;
import com.smartshelfx.smartshelfx_backend.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final StockRequestRepository stockRequestRepository;
    private static final Logger log = LoggerFactory.getLogger(StockService.class);
    // INTEGRATION: Inject the AlertService to trigger notifications
    private final AlertService alertService;
    @Transactional
    public void logTransaction(Product product, Integer qty, String type, String reason, Long warehouseId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("DEBUG: Preparing to log transaction for product {} qty {}", product.getName(), qty);
        StockTransaction tx = new StockTransaction(); 
        tx.setProduct(product);
        tx.setQuantity(qty);
        tx.setType(type); // "IN" or "OUT"
        tx.setReason(reason);
        tx.setWarehouseId(warehouseId); 
        tx.setTimestamp(LocalDateTime.now());
        
        userRepository.findByEmail(email).ifPresent(tx::setHandledBy);
        System.out.println("DEBUG: Preparing to log transaction for product " + product.getName() + " of type " + type + " qty " + qty);
        transactionRepository.save(tx);
        System.out.println("DEBUG: Logged transaction for product " + product.getName() + " of type " + type + " qty " + qty);
        // Always sync and check alerts after any change
        syncGlobalProductStock(product.getId());
        System.out.println("DEBUG: Completed sync and alert check for product " + product.getName());
    }

    public void createAiStockRequest(String productName, Integer quantity, String userEmail) {
        Product product = productRepository.findByName(productName)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productName));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        StockRequest request = StockRequest.builder()
                .product(product)
                .requestedQuantity(quantity)
                .status("PENDING")
                .warehouseId(1L) 
                .managerId(user.getId())
                .managerNotes("AI Auto-Generated Demand Request")
                .build();

        stockRequestRepository.save(request);


        alertService.createVendorNotification(
            product.getVendor().getId(), 
            "AI Prediction", 
            "AI suggests a restock of " + quantity + " units for " + product.getName()
        );

        alertService.createManagerNotification(
            1L, 
            "AI Stock Request Created", 
            "A new AI-generated stock request for " + product.getName() + " (" + quantity + " units) has been created."
        );
    }


public void syncGlobalProductStock(Long productId) {
    log.info("DEBUG: Syncing stock for Product ID: {}", productId);
    Integer totalQuantity = warehouseStockRepository.getTotalStockByProductId(productId);
    
    productRepository.findById(productId).ifPresent(product -> {
        product.setCurrentStock(totalQuantity != null ? totalQuantity : 0);
        // saveAndFlush forces the DB to update immediately so alertService sees the new value
        productRepository.saveAndFlush(product); 

        log.info("DEBUG: Stock is now {}. Triggering alert check...", product.getCurrentStock());
        alertService.generateLowStockAlert(product);

        alertService.createVendorNotification(
            product.getVendor().getId(), 
            "Low Stock Alert", 
            "Low stock detected for " + product.getName() + ". Current stock is " + totalQuantity 
        );

        
    });
}
}
