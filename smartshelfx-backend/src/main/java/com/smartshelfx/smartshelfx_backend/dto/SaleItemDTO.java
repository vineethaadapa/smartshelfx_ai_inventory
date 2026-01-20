package com.smartshelfx.smartshelfx_backend.dto;

import java.time.LocalDateTime;

public class SaleItemDTO {
    private String productName;
    private Integer quantitySold;
    private Double priceAtSale;
    private Double totalItemRevenue;
    private LocalDateTime saleDate;

    // Default Constructor
    public SaleItemDTO() {}

    // All-args Constructor
    public SaleItemDTO(String productName, Integer quantitySold, Double priceAtSale, LocalDateTime saleDate) {
        this.productName = productName;
        this.quantitySold = quantitySold;
        this.priceAtSale = priceAtSale;
        this.totalItemRevenue = quantitySold * priceAtSale;
        this.saleDate = saleDate;
    }

    // Getters and Setters
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantitySold() { return quantitySold; }
    public void setQuantitySold(Integer quantitySold) { this.quantitySold = quantitySold; }

    public Double getPriceAtSale() { return priceAtSale; }
    public void setPriceAtSale(Double priceAtSale) { this.priceAtSale = priceAtSale; }

    public Double getTotalItemRevenue() { return totalItemRevenue; }
    public void setTotalItemRevenue(Double totalItemRevenue) { this.totalItemRevenue = totalItemRevenue; }

    public LocalDateTime getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }
}