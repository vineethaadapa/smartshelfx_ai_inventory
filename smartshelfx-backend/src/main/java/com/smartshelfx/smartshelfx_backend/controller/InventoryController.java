package com.smartshelfx.smartshelfx_backend.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartshelfx.smartshelfx_backend.Repository.ProductRepository;
import com.smartshelfx.smartshelfx_backend.Service.ReportService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:4200") // This allows your Angular app to connect
public class InventoryController {

    @Autowired
    private ProductRepository productRepository;

    @PostMapping("/update-stock")
    public ResponseEntity<?> updateStock(@RequestBody Map<String, Object> payload) {
        String sku = (String) payload.get("sku");
        Integer quantityToAdd = (Integer) payload.get("quantityToAdd");

        // Logic to find product and update currentStock goes here
        return ResponseEntity.ok(Map.of("message", "Stock updated successfully"));
    }
}