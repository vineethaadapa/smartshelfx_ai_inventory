

package com.smartshelfx.smartshelfx_backend.dto;

import lombok.Data;

@Data // Lombok: Generates getters and setters
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String role;
}