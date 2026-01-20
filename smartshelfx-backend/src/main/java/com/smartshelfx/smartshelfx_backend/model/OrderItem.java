package com.smartshelfx.smartshelfx_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Data // If using Lombok, otherwise generate getters/setters
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product; // This links the sale to the Vendor's product

    private Integer quantitySold;
    
    private Double priceAtSale; // Important: The price at the time of purchase

    private LocalDateTime saleDate;

    private Long orderId; // Reference to the main Order ID

    
}
