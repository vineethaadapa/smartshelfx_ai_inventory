package com.smartshelfx.smartshelfx_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long warehouseId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer currentStock;
    private Integer reorderLevel;
}