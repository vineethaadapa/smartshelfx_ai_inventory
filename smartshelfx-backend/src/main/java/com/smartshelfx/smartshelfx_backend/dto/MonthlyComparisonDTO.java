package com.smartshelfx.smartshelfx_backend.dto;

public class MonthlyComparisonDTO {
    private String month;
    private double purchases;
    private double sales;

    public MonthlyComparisonDTO(String month, double purchases, double sales) {
        this.month = month;
        this.purchases = purchases;
        this.sales = sales;
    }

    // Getters and setters
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public double getPurchases() { return purchases; }
    public void setPurchases(double purchases) { this.purchases = purchases; }

    public double getSales() { return sales; }
    public void setSales(double sales) { this.sales = sales; }
}