package com.smartshelfx.smartshelfx_backend.model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @Column(length = 1000) 
    private String description;

    private String sku;
    private String category;
    private Integer reorderLevel;
    private Double price;

    private String imageUrl;


    private boolean deleted = false;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock = 0; 

    @ManyToOne
    @JoinColumn(name = "vendor_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"authorities", "password", "enabled"})
    private User vendor;

    @Column(name = "vendor_id")
    private Long vendorId;

    private LocalDate expiryDate;

    public Integer getStock() {
        return this.currentStock;
    }

    public void setStock(Integer stock) {
        this.currentStock = stock;
    }

}