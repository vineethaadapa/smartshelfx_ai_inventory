package com.smartshelfx.smartshelfx_backend.Config;

import com.smartshelfx.smartshelfx_backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) 
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/api/stock/**").hasAnyAuthority("ADMIN", "MANAGER")
                .requestMatchers("/api/vendor/**").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/api/inventory/**").hasAnyAuthority("VENDOR", "ADMIN")
                .requestMatchers("/api/products/vendor/**").permitAll()
                .requestMatchers("/api/predictions/**").hasAnyAuthority("ADMIN", "VENDOR", "MANAGER")
                .requestMatchers("/api/stocks/ai-reorder").hasAnyAuthority("ADMIN", "MANAGER")
                .requestMatchers("/api/vendor/my-products/**").hasAnyAuthority("VENDOR", "ROLE_VENDOR", "ADMIN")
                .requestMatchers("/api/vendor/reorders/**").hasAnyAuthority("VENDOR", "ROLE_VENDOR", "ADMIN")
                .requestMatchers("/api/vendor/sales-report/**").hasAnyAuthority("VENDOR", "ROLE_VENDOR")
                .requestMatchers("/api/vendor/**").hasAnyAuthority("VENDOR", "ROLE_VENDOR", "ADMIN") 
                .requestMatchers("/api/orders/**").permitAll()
                .requestMatchers("/api/alerts/all").hasAnyAuthority("ADMIN", "MANAGER")
                .requestMatchers("/api/alerts/mark-read/**").hasAnyAuthority("ADMIN", "MANAGER", "VENDOR" , "ROLE_VENDOR")
                .requestMatchers("/api/alerts/**").hasAnyAuthority("ADMIN", "MANAGER", "VENDOR")
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                .requestMatchers(HttpMethod.PUT, "/api/alerts/mark-read/**").hasAnyAuthority("VENDOR", "ROLE_VENDOR", "ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/alerts/*/dismiss").hasAnyAuthority("VENDOR", "ROLE_VENDOR", "ADMIN", "MANAGER")
                .requestMatchers("/api/alerts/**").hasAnyAuthority("ADMIN", "MANAGER", "VENDOR")
                .requestMatchers("/api/admin/export-pdf").hasAuthority("ADMIN")
                .requestMatchers("/api/admin/inventory-trends").hasAuthority("ADMIN")
                .requestMatchers("/api/admin/monthly-comparison").hasAuthority("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200","https://smartshelfxdb.netlify.app/login")); 
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}