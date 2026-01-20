

package com.smartshelfx.smartshelfx_backend.controller;

import com.smartshelfx.smartshelfx_backend.dto.AuthResponse;
import com.smartshelfx.smartshelfx_backend.dto.LoginRequest;
import com.smartshelfx.smartshelfx_backend.dto.RegisterRequest;
import com.smartshelfx.smartshelfx_backend.Service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth") // Base path for all authentication endpoints
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED); // Returns 201 Created on success
        } catch (RuntimeException e) {
            // Catch the "Email already taken!" exception thrown by the service
            return new ResponseEntity<>(new AuthResponse(null, request.getEmail(), e.getMessage()), 
                                        HttpStatus.BAD_REQUEST); // Returns 400 Bad Request
        }
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response); // Returns 200 OK on success
        } catch (RuntimeException e) {
            // Catch authentication failures (e.g., bad credentials)
            return new ResponseEntity<>(new AuthResponse(null, request.getEmail(), "Invalid credentials"), 
                                        HttpStatus.UNAUTHORIZED); // Returns 401 Unauthorized
        }
    }
}