package com.hackathon.hackathon.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private static String SECRET_KEY;

    @Value("${JWT_SECRET_KEY}")
    public void setSecretKey(String secretKey) {
        JwtUtil.SECRET_KEY = secretKey;
    }

    public static String generateToken(String email, String role, String userId, String fullName) {
        return Jwts.builder().subject(email).claim("role", role).claim("userId", userId)
                .claim("fullName", fullName).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                // Standard for Unix and Windows OS.
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        SECRET_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();
    }

    // ĐÃ KHÔI PHỤC LẠI static - Hoàn toàn tương thích với code cũ của bạn
    public static Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        SECRET_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token).getPayload();
    }
}
