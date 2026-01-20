package com.smartshelfx.smartshelfx_backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The product being requested
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private Integer requestedQuantity;

    // Status can be: PENDING, APPROVED, REJECTED, COMPLETED
    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false)
    private Long managerId;

    private String managerNotes;

    private LocalDateTime requestDate;

    // Automatically set the date when a request is created
    @PrePersist
    protected void onCreate() {
        this.requestDate = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }

    private LocalDateTime responseDate; 

}