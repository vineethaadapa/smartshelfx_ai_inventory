package com.smartshelfx.smartshelfx_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "userRole") // Matches your manual SQL success
    private String userRole; 

    private String type;     
    private String priority; 
    private String message;
    private String productSku;
    private Long vendorId;  
    private Long warehouseId; 
    private String status = "ACTIVE";


    public String getStatus() {
        return status;
    }

    @Column(name = "isRead") // Matches camelCase
    private boolean isRead = false;

    @Column(name = "createdAt") // Matches camelCase
    private LocalDateTime createdAt = LocalDateTime.now();
}