package com.smartshelfx.smartshelfx_backend.dto;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;

    @Data
    public class ProductImportDTO {
        @CsvBindByName(column = "SKU") 
        private String sku;

        @CsvBindByName(column = "Name") 
        private String name;

        @CsvBindByName(column = "Category") 
        private String category;

        @CsvBindByName(column = "Stock") 
        private Integer stock;

        @CsvBindByName(column = "Price") 
        private Double price;
    }
