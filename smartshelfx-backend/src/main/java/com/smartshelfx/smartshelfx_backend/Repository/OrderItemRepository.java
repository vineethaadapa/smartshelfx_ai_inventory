package com.smartshelfx.smartshelfx_backend.Repository;

import com.smartshelfx.smartshelfx_backend.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

   List<OrderItem> findByProduct_Vendor_Email(String email);
}