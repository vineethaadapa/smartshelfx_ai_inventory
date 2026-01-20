

package com.smartshelfx.smartshelfx_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token; // The JWT token the frontend will use for future requests
    private String username;
    private String message;
}