

package com.smartshelfx.smartshelfx_backend.controller;

import com.smartshelfx.smartshelfx_backend.model.Alert;
import com.smartshelfx.smartshelfx_backend.Service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "http://localhost:4200")
public class AlertController {

    @Autowired
    private AlertService alertService;

    // 1. FOR THE BELL ICON: Returns only unread alerts
    @GetMapping("/unread/{role}")
    public ResponseEntity<List<Alert>> getUnreadAlerts(@PathVariable String role) {
        return ResponseEntity.ok(alertService.getUnreadAlertsForBell(role));
    }
    
    // 2. FOR THE ALERTS TAB: Returns all history (Read + Unread)
    @GetMapping("/role/{role}") 
    public ResponseEntity<List<Alert>> getAlertHistory(@PathVariable String role) {
        return ResponseEntity.ok(alertService.getAllAlertsByRole(role)); 
    }

    // 3. VENDOR ALERTS
    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<Alert>> getVendorAlerts(@PathVariable Long vendorId) {
        return ResponseEntity.ok(alertService.getAlertsForVendor(vendorId));
    }

    // 4. MARK AS READ: Unified endpoint
    @PutMapping("/mark-read/{id}") 
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        alertService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    // Alias for dismiss if your frontend uses both
    @PutMapping("/{id}/dismiss")
    public ResponseEntity<Void> dismissAlert(@PathVariable Long id) {
        alertService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/manager/{warehouseId}")
    public List<Alert> getManagerAlerts(@PathVariable Long warehouseId) {
        return alertService.getAlertsForManager(warehouseId);
    }

}