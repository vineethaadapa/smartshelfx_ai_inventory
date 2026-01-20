package com.smartshelfx.smartshelfx_backend.dto;

public class InventoryTrendDTO {
    private String date;
    private Double totalStock;

    public InventoryTrendDTO(String date, Double totalStock) {
        this.date = date;
        this.totalStock = totalStock;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Double getTotalStock() { return totalStock; }
    public void setTotalStock(Double totalStock) { this.totalStock = totalStock; }
}