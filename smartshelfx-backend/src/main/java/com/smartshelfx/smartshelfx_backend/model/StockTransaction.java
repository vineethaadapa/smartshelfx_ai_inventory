package com.smartshelfx.smartshelfx_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transactions")
@Data
@Builder
@NoArgsConstructor  // Needed for JPA and "new StockTransaction()"
@AllArgsConstructor // Needed for @Builder
public class StockTransaction {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer quantity;
    private String type; // IN, OUT, ADJUSTMENT, DELETE
    private String reason;
    private Long warehouseId; 
    private Long managerId;
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "handled_by")
    private User handledBy;
}