

package com.smartshelfx.smartshelfx_backend.Service;

import com.smartshelfx.smartshelfx_backend.dto.AuthResponse;
import com.smartshelfx.smartshelfx_backend.dto.LoginRequest;
import com.smartshelfx.smartshelfx_backend.dto.RegisterRequest;
import com.smartshelfx.smartshelfx_backend.model.User;
import com.smartshelfx.smartshelfx_backend.Repository.UserRepository;
import com.smartshelfx.smartshelfx_backend.security.JwtService; // ⬅️ NEW IMPORT
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager; // ⬅️ NEW IMPORT
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // ⬅️ NEW IMPORT
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder; // ⬅️ NEW IMPORT
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor 
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Injected from SecurityConfig
    private final JwtService jwtService; // Injected from Step 14
    private final AuthenticationManager authenticationManager; // Injected from SecurityConfig

    @Override
    public AuthResponse register(RegisterRequest request) {
        // 1. Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            // Throw an exception that AuthController will catch and return a 400
            throw new RuntimeException("Email is already taken!");
        }

        // 2. Build and save the new User entity
        var user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                // 3. Hash the password before saving
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole().toUpperCase() : "VENDOR")
                .build();

        User savedUser = userRepository.save(user);

        // 4. Generate JWT token for immediate login
        var jwtToken = jwtService.generateToken(savedUser);

        // 5. Return the token and user details to the frontend
        return new AuthResponse(jwtToken, savedUser.getEmail(), "Registration successful");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            // 1. Authenticate the user credentials using AuthenticationManager
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
             // Throw an exception for failed credentials (caught by AuthController)
            throw new RuntimeException("Authentication failed: Invalid email or password");
        }

        // 2. Load the authenticated user details
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found")); // Should not happen if auth succeeded

        // 3. Generate JWT token
        var jwtToken = jwtService.generateToken(user);

        // 4. Return the token
        return new AuthResponse(jwtToken, user.getEmail(), "Login successful");
    }
}