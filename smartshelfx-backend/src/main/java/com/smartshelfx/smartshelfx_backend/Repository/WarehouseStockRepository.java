package com.smartshelfx.smartshelfx_backend.Repository;

import com.smartshelfx.smartshelfx_backend.model.WarehouseStock;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {
    
    // Finds all products in a specific manager's warehouse
    List<WarehouseStock> findByWarehouseId(Long warehouseId);

    // Used to update a specific product's stock in a specific warehouse
    Optional<WarehouseStock> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    @Query("SELECT SUM(ws.currentStock) FROM WarehouseStock ws WHERE ws.product.id = :productId")
    Integer getTotalStockByProductId(@Param("productId") Long productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM WarehouseStock ws WHERE ws.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}