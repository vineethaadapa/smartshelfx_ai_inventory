package com.smartshelfx.smartshelfx_backend.Repository;

import com.smartshelfx.smartshelfx_backend.model.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    // Fixes "method findByWarehouseId(Long) is undefined"
    List<StockTransaction> findByWarehouseId(Long warehouseId);

    // Used for the Admin Audit Log
    List<StockTransaction> findAllByOrderByTimestampDesc();

    @Query("SELECT t FROM StockTransaction t WHERE t.product.vendor.id = :vendorId AND t.type = 'OUT' ORDER BY t.timestamp DESC")
    List<StockTransaction> findSalesByVendorId(@Param("vendorId") Long vendorId);

    StockTransaction findTopByWarehouseIdOrderByTimestampDesc(Long warehouseId);

    @Query(value = "SELECT DATE(timestamp) as date, SUM(quantity) as amount " +
                   "FROM stock_transactions " +
                   "GROUP BY DATE(timestamp) " +
                   "ORDER BY date ASC", nativeQuery = true)
    List<Object[]> findDailyStockChanges();

    @Query(value = "SELECT DATE_FORMAT(timestamp, '%Y-%m') as month, " +
        "SUM(CASE WHEN type = 'IN' or type = 'adjustment' THEN quantity ELSE 0 END) as purchases,  " +
        "SUM(CASE WHEN type = 'OUT' THEN quantity ELSE 0 END) as sales " +
        "FROM stock_transactions GROUP BY month ORDER BY month ASC", 
        nativeQuery = true)
    List<Object[]> findMonthlyComparison();
}