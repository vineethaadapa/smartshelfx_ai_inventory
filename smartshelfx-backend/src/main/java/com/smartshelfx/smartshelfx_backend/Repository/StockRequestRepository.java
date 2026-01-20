package com.smartshelfx.smartshelfx_backend.Repository;

import com.smartshelfx.smartshelfx_backend.model.Product;
import com.smartshelfx.smartshelfx_backend.model.StockRequest;
import com.smartshelfx.smartshelfx_backend.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockRequestRepository extends JpaRepository<StockRequest, Long> {
    // Find all requests made by a specific warehouse
    List<StockRequest> findByWarehouseId(Long warehouseId);
    
    // Optional: Find only pending requests for the Admin to see
    List<StockRequest> findByStatus(String status);

    List<StockRequest> findByProductVendor(User vendor);

    // Alternative using a manual Query if you prefer:
    @Query("SELECT r FROM StockRequest r WHERE r.product.vendor = :vendor")
    List<StockRequest> findByVendor(@Param("vendor") User vendor);

    @Query("SELECT r FROM StockRequest r WHERE r.product.vendor = :vendor")
    List<StockRequest> findRequestsByVendor(@Param("vendor") User vendor);
}