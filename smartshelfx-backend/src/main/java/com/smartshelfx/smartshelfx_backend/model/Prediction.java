package com.smartshelfx.smartshelfx_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "predictions")
@Data
public class Prediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private Double predictedDemand;
    private String predictionDate; 
    
    @Column(name = "vendor_id")
    private Long vendorId; 

    private String sku;        
    private Double unitPrice;  
    @JsonProperty("prediction_value") 
    private Double predictionValue;
}