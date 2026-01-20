package com.smartshelfx.smartshelfx_backend.controller;

import com.smartshelfx.smartshelfx_backend.Service.POService;
import com.smartshelfx.smartshelfx_backend.model.PurchaseOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200") // Ensure Angular can connect
public class PurchaseOrderController {

    private final POService poService;

    @PostMapping("/generate")
    public ResponseEntity<PurchaseOrder> generatePO(@RequestBody PurchaseOrder order) {
        // We delegate all logic to the Service
        return ResponseEntity.ok(poService.createOrder(order));
    }

    @GetMapping("/all")
    public ResponseEntity<List<PurchaseOrder>> getAllOrders() {
        return ResponseEntity.ok(poService.getAllOrders());
    }

}