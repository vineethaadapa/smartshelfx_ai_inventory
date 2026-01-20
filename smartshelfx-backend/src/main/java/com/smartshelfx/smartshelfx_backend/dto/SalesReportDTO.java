package com.smartshelfx.smartshelfx_backend.dto;

import java.util.List;

public class SalesReportDTO {
    private List<SaleItemDTO> items;
    private Double totalRevenue;

    // Constructor
    public SalesReportDTO(List<SaleItemDTO> items, Double totalRevenue) {
        this.items = items;
        this.totalRevenue = totalRevenue;
    }

    // Getters and Setters
    public List<SaleItemDTO> getItems() { return items; }
    public void setItems(List<SaleItemDTO> items) { this.items = items; }

    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
}