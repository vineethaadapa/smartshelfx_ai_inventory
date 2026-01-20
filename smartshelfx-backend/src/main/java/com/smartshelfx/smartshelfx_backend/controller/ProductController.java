package com.smartshelfx.smartshelfx_backend.controller;

import com.smartshelfx.smartshelfx_backend.model.Product;
import com.smartshelfx.smartshelfx_backend.Repository.ProductRepository;
import com.smartshelfx.smartshelfx_backend.Repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;
     private UserRepository userRepository;
    @GetMapping("/products")
    public List<Product> getAllProducts() {
        // This will print to your IDE console so you can see if the DB has data
        List<Product> products = productRepository.findByDeletedFalse();
        System.out.println("DEBUG: Sending " + products.size() + " products to frontend.");
        return products;
    }

    @GetMapping("/products/vendor/{vendorId}")
    public List<Product> getProductsByVendor(@PathVariable Long vendorId) {
        List<Product> list = productRepository.findByVendorId(vendorId);
        System.out.println("Vendor ID " + vendorId + " has " + list.size() + " products in DB.");
        return list;
    }
}