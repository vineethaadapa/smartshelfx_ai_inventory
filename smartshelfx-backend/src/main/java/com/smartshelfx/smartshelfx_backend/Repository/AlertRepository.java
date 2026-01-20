package com.smartshelfx.smartshelfx_backend.Repository;

import com.smartshelfx.smartshelfx_backend.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    // For Admin and Warehouse Manager
    List<Alert> findByUserRoleOrderByCreatedAtDesc(String userRole);

    // For Vendors (Filter by their specific vendorId)
    List<Alert> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
    
    // For the UI Badge (count of unread notifications)
    long countByUserRoleAndIsReadFalse(String userRole);

    boolean existsByProductSkuAndUserRoleAndIsReadFalse(String productSku, String userRole);

    List<Alert> findByUserRoleAndIsReadFalseOrderByCreatedAtDesc(String role);

    List<Alert> findByUserRoleAndIsReadTrueOrderByCreatedAtDesc(String role);

    List<Alert> findByVendorIdAndIsReadFalseOrderByCreatedAtDesc(Long vendorId);

    List<Alert> findByWarehouseIdAndIsReadFalseAndStatus(Long warehouseId, String status);
}