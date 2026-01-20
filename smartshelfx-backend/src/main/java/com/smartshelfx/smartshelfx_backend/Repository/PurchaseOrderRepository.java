package com.smartshelfx.smartshelfx_backend.Repository;

import com.smartshelfx.smartshelfx_backend.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    
    // Find orders by their status (PENDING, APPROVED, etc.)
    List<PurchaseOrder> findByStatus(String status);

    // Find all orders assigned to a specific vendor
    List<PurchaseOrder> findByVendorId(Long vendorId);

    List<PurchaseOrder> findByVendorEmail(String email);
}