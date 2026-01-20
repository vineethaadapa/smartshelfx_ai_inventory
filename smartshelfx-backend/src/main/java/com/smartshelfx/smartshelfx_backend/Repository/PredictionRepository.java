package com.smartshelfx.smartshelfx_backend.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smartshelfx.smartshelfx_backend.model.Prediction;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    
    // Use findByVendorId to match the "private Long vendorId" field in Prediction.java
    List<Prediction> findByVendorId(Long vendorId);
}
