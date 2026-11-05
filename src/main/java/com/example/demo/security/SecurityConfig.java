package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import com.example.demo.auth.JwtService;
import com.example.demo.user.CustomUserDetailsService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
                    // The container's error dispatch must stay public, otherwise every 4xx/5xx
                    // is rewritten to an empty 403 before the client can read the message.
                    .requestMatchers("/error").permitAll()
                    // User management: admins manage accounts, lecturers onboard their students.
                    .requestMatchers(HttpMethod.POST, "/api/users").hasAnyRole("ADMIN", "LECTURER")
                    // Course + enrolment management.
                    .requestMatchers(HttpMethod.POST, "/api/courses").hasAnyRole("ADMIN", "LECTURER")
                    .requestMatchers(HttpMethod.POST, "/api/courses/*/members").hasAnyRole("ADMIN", "LECTURER")
                    .requestMatchers(HttpMethod.PATCH, "/api/courses/**").hasAnyRole("ADMIN", "LECTURER")
                    .requestMatchers(HttpMethod.DELETE, "/api/courses/**").hasAnyRole("ADMIN", "LECTURER")
                    // Assignment authoring.
                    .requestMatchers(HttpMethod.POST, "/api/courses/*/assignments").hasAnyRole("ADMIN", "LECTURER")
                    .requestMatchers(HttpMethod.PATCH, "/api/assignments/**").hasAnyRole("ADMIN", "LECTURER")
                    .requestMatchers(HttpMethod.DELETE, "/api/assignments/**").hasAnyRole("ADMIN", "LECTURER")
                    // Test cases carry hidden grading inputs, so even reads are lecturer-only.
                    .requestMatchers("/api/assignments/*/tests/**").hasAnyRole("ADMIN", "LECTURER")
                    .anyRequest().authenticated())
            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:[*]",
                "http://127.0.0.1:[*]"
        ));
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"
        ));
        configuration.setExposedHeaders(List.of(
                "Authorization"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {

        return new JwtAuthenticationFilter(
                jwtService,
                userDetailsService);
    }
}