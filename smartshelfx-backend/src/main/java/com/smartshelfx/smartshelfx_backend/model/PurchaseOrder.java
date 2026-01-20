

package com.smartshelfx.smartshelfx_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_orders")
@Data
public class PurchaseOrder {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String poNumber; // Unique ID (e.g., PO-A1B2C3D4)

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    private User vendor;

    // Added fields for Module 5
    private Integer quantity;
    private Double unitPrice;
    private Double totalAmount;
    

    private String status; 

    private LocalDateTime createdAt = LocalDateTime.now();

    private String targetWarehouse; 
}