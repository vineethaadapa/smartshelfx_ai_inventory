

package com.smartshelfx.smartshelfx_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder; 
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority; 
import org.springframework.security.core.authority.SimpleGrantedAuthority; 
import org.springframework.security.core.userdetails.UserDetails; 
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users") 
@Data
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 
public class User implements UserDetails { 

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🚨 FIX 1: Explicitly defining the missing fields
    @Column(nullable = false)
    private String name; 

    @Column(unique = true, nullable = false)
    private String email; 

    @Column(nullable = false)
    private String password; 

    @Builder.Default // ⬅️ ADD THIS ANNOTATION
    private String role = "USER"; 

    private Long warehouseId; // NEW: To track which warehouse the manager works at
    
    // ----------------------------------------------------
    // Implementation of UserDetails methods
    // ----------------------------------------------------

    // 🚨 FIX 2: Implement the missing getPassword() method (required by UserDetails)
    @Override
    public String getPassword() {
        return password;
    }

    // Maps the user's role string to a Spring Security GrantedAuthority
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 🚨 FIX 3: List.of(E) is correct for the arguments (SimpleGrantedAuthority)
        return List.of(new SimpleGrantedAuthority(this.role));
    }

    // The method Spring Security uses to get the user's username (we use email)
    @Override
    public String getUsername() {
        return email;
    }

    // --- Account status methods (we set them to true/enabled by default) ---
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}