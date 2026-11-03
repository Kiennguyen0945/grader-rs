package com.example.demo.auth;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.security.core.GrantedAuthority;
import com.example.demo.user.CustomUserPrincipal;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final SecretKey key = Keys.hmacShaKeyFor("change-this-to-a-long-secret-key-at-least-32-bytes"
    .getBytes(StandardCharsets.UTF_8));
    public String generateAccessToken(CustomUserPrincipal user) {
        return  Jwts.builder()
        .subject(user.getId().toString())
        .claim("roles", user.getAuthorities()
        .stream()
        .map(GrantedAuthority::getAuthority)
        .toList())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
        .signWith(key)
        .compact();
    }

    public String extractUserId(String token) {
        System.out.println("EXTRACT USER ID");
        return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
    }

    public boolean isValid(String token, CustomUserPrincipal user) { 
        System.out.println("User id is valid");
        return extractUserId(token).equals(user.getId().toString());
    }
}
