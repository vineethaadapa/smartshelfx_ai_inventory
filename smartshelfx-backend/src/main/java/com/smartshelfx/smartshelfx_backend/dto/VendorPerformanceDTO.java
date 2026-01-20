package com.smartshelfx.smartshelfx_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VendorPerformanceDTO {
    private String vendorName;
    private long totalRequests;
    private long approvedRequests;
    private double fulfillmentRate; 
    private double avgResponseTimeHours; 
}
