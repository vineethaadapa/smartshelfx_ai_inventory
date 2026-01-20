package com.smartshelfx.smartshelfx_backend.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smartshelfx.smartshelfx_backend.Repository.StockRequestRepository;
import com.smartshelfx.smartshelfx_backend.Repository.UserRepository;
import com.smartshelfx.smartshelfx_backend.dto.VendorPerformanceDTO;
import com.smartshelfx.smartshelfx_backend.model.StockRequest;
import com.smartshelfx.smartshelfx_backend.model.User;

@Service
public class VendorService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRequestRepository stockRequestRepository;

    public List<VendorPerformanceDTO> getVendorPerformanceReport() {
        // Find all users with the role VENDOR
        List<User> vendors = userRepository.findAllByRole("VENDOR");
        
        return vendors.stream().map(vendor -> {
            // Get all requests for this specific vendor's products
            List<StockRequest> requests = stockRequestRepository.findByProductVendor(vendor);
            
            long total = requests.size();
            long approved = requests.stream()
                .filter(r -> "APPROVED".equals(r.getStatus())).count();
            
            // Calculate response time only if both dates exist
            double avgTime = requests.stream()
                .filter(r -> r.getResponseDate() != null && r.getRequestDate() != null)
                .mapToLong(r -> java.time.Duration.between(r.getRequestDate(), r.getResponseDate()).toHours())
                .average().orElse(0.0);

            return new VendorPerformanceDTO(
                vendor.getName(),
                total,
                approved,
                total > 0 ? (double) approved / total * 100 : 0,
                avgTime
            );
        }).collect(Collectors.toList());
    }
}