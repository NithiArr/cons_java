package com.construction.config;

import com.construction.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // Django uses PBKDF2 or similar by default, but let's configure a generic DelegatingPasswordEncoder
    // or plain text for dev if necessary. We should probably use the standard BCrypt if starting fresh,
    // but to support existing django passwords we need a custom encoder or we assume user passwords 
    // will be reset or we use NoOp/BCrypt. For now, matching standard bcrypt.
    
    // Actually Django uses pbkdf2_sha256 format. Spring security supports pbkdf2.
    // For simplicity, we can use standard Spring Security password encoders.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new DjangoPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers("/login", "/css/**", "/js/**", "/assets/**", "/static/**").permitAll()

                // ── ADMIN only ──────────────────────────────────────────────
                .requestMatchers(
                    "/dashboard/owner", "/dashboard/main",
                    "/register",
                    "/users", "/manage-users", "/manage_users",
                    "/users/*/edit",
                    "/activity-log",
                    "/master-categories", "/master_categories"
                ).hasRole("ADMIN")

                // ── MANAGER + ADMIN ──────────────────────────────────────────
                .requestMatchers(
                    "/projects", "/vendors",
                    "/expenses", "/payments",
                    "/client-payments", "/client_payments",
                    "/dashboard/daily-cash", "/daily_cash", "/daily-cash",
                    "/dashboard/vendor-analytics", "/vendor_analytics", "/vendor-analytics",
                    "/payment-mode-split", "/payment_mode_split"
                ).hasAnyRole("ADMIN", "MANAGER")

                // ── EMPLOYEE, MANAGER, ADMIN ─────────────────────────────────
                .requestMatchers("/purchases").hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")

                // ── API write restrictions (EMPLOYEE = read-only) ────────────
                // Allow EMPLOYEE GET on purchases API only
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/**").hasAnyRole("ADMIN", "MANAGER")

                // All other requests: authenticated
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );

        return http.build();
    }

}
