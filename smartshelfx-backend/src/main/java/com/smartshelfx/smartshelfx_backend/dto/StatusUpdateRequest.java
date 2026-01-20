package com.smartshelfx.smartshelfx_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {
    private String status;
}