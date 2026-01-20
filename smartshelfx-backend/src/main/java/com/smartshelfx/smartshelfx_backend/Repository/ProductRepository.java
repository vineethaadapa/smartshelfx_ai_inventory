package com.smartshelfx.smartshelfx_backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.smartshelfx.smartshelfx_backend.model.Product;
import com.smartshelfx.smartshelfx_backend.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Used by Manager Dashboard to show the list of items to update
    List<Product> findByDeletedFalse();
    
    // Used for the "Low Stock Alerts" card on the dashboard
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.currentStock <= p.reorderLevel")
    List<Product> findLowStockProducts();

    // Find products by category for filtering
    List<Product> findByCategoryAndDeletedFalse(String category);

    Optional<Product> findBySku(String sku);

    List<Product> findByVendorAndDeletedFalse(User vendor);

    Optional<Product> findByName(String name);

    List<Product> findByExpiryDateBefore(LocalDate date);
    
    @Query("SELECT p FROM Product p WHERE p.vendor.id = :vendorId")
    List<Product> findByVendorId(@Param("vendorId") Long vendorId);

    boolean existsBySku(String sku);
}