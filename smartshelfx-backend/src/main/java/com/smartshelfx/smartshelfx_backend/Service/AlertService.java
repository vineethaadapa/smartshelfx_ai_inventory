package com.smartshelfx.smartshelfx_backend.Service;

import com.smartshelfx.smartshelfx_backend.model.Alert;
import com.smartshelfx.smartshelfx_backend.model.Product;
import com.smartshelfx.smartshelfx_backend.Repository.AlertRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    // 1. FOR THE BELL: Only get what hasn't been read
    public List<Alert> getUnreadAlertsForBell(String role) {
        return alertRepository.findByUserRoleAndIsReadFalseOrderByCreatedAtDesc(role);
    }

    // 2. FOR THE TAB: Get everything (History: both Read and Unread)
    public List<Alert> getAllAlertsByRole(String role) {
        return alertRepository.findByUserRoleOrderByCreatedAtDesc(role);
    }

    @Transactional 
    public void markAsRead(Long id) {
        alertRepository.findById(id).ifPresent(alert -> {
            alert.setRead(true); 
            alertRepository.save(alert);
            System.out.println("DEBUG: Alert ID " + id + " updated to READ in Database.");
        });
    }

    public void generateLowStockAlert(Product product) {
        if (product.getCurrentStock() <= product.getReorderLevel()) {
            boolean alreadyNotified = alertRepository.existsByProductSkuAndUserRoleAndIsReadFalse(
                product.getSku(), "ADMIN"
            );

            if (!alreadyNotified) {
                Alert adminAlert = new Alert();
                adminAlert.setUserRole("ADMIN");
                adminAlert.setType("LOW_STOCK");
                adminAlert.setPriority("HIGH");
                adminAlert.setProductSku(product.getSku());
                adminAlert.setMessage("Urgent: " + product.getName() + " is low (" + product.getCurrentStock() + ").");
                adminAlert.setRead(false); 
                alertRepository.save(adminAlert);
            }
        }
    }
    
    // Vendor specific alerts
    public List<Alert> getAlertsForVendor(Long vendorId) {
        return alertRepository.findByVendorIdAndIsReadFalseOrderByCreatedAtDesc(vendorId);
    }

    public void createVendorNotification(Long vendorId, String type, String message) {
        Alert alert = new Alert();
        alert.setVendorId(vendorId);
        alert.setUserRole("VENDOR");
        alert.setType(type);
        alert.setPriority("MEDIUM");
        alert.setMessage(message + "");
        alertRepository.save(alert);
    }

    public void createManagerNotification(Long warehouseId, String type, String message) {
        Alert alert = new Alert();
        alert.setWarehouseId(warehouseId);
        alert.setUserRole("MANAGER");
        alert.setType(type);
        alert.setPriority("MEDIUM");
        alert.setMessage(message);
        alertRepository.save(alert);
    }

    public List<Alert> getAlertsForManager(Long warehouseId) {
        // Return only 'ACTIVE' alerts for this warehouse
        return alertRepository.findByWarehouseIdAndIsReadFalseAndStatus(warehouseId, "ACTIVE");
    }

    public void dismissAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Alert not found"));
        
        // Update status to DISMISSED instead of deleting
        alert.setStatus("DISMISSED");
        alertRepository.save(alert);
    }
}