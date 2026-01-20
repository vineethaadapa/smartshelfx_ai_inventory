

package com.smartshelfx.smartshelfx_backend.Service;

import com.smartshelfx.smartshelfx_backend.dto.AuthResponse;
import com.smartshelfx.smartshelfx_backend.dto.LoginRequest;
import com.smartshelfx.smartshelfx_backend.dto.RegisterRequest;

public interface AuthService {

    // Method for registering a new user
    AuthResponse register(RegisterRequest request);

    // Method for logging in an existing user
    AuthResponse login(LoginRequest request);
}