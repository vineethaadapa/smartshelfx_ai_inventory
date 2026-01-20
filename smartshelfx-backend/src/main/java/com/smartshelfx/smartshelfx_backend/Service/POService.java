package com.smartshelfx.smartshelfx_backend.Service;

import com.smartshelfx.smartshelfx_backend.Repository.PurchaseOrderRepository;
import com.smartshelfx.smartshelfx_backend.model.PurchaseOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class POService {

    private final PurchaseOrderRepository poRepository;

    public PurchaseOrder createOrder(PurchaseOrder order) {
        // 1. Generate a Unique Purchase Order Number
        String poRef = "PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        order.setPoNumber(poRef);

        // 2. Set Status and Timestamps
        order.setStatus("PENDING_VENDOR");
        order.setCreatedAt(LocalDateTime.now());

        // 3. Calculation logic (Quantity * UnitPrice) 
        // Note: Ensure your PurchaseOrder model has these fields
        if (order.getQuantity() != null && order.getUnitPrice() != null) {
            order.setTotalAmount(order.getQuantity() * order.getUnitPrice());
        }

        return poRepository.save(order);
    }

    public List<PurchaseOrder> getAllOrders() {
        return poRepository.findAll();
    }

    public List<PurchaseOrder> getOrdersByVendorEmail(String email) {
        return poRepository.findByVendorEmail(email);
    }
}