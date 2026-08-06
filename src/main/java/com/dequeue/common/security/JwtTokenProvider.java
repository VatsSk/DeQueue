package com.dequeue.common.security;

import com.dequeue.staff.entity.Staff;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;
    
    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserPrincipal userPrincipal) {
        return buildToken(userPrincipal.getId(), userPrincipal.getVendorId(),
                userPrincipal.getEmail(), userPrincipal.getRole(),
                userPrincipal.getDepartment(), jwtExpirationMs);
    }

    public String generateToken(Staff staff) {
        return buildToken(staff.getId(), staff.getVendorId(), staff.getEmail(),
                staff.getRole() != null ? staff.getRole().name() : null,
                staff.getDepartmentId(), jwtExpirationMs);
    }

    public String generateRefreshToken(UserPrincipal userPrincipal) {
        return buildToken(userPrincipal.getId(), userPrincipal.getVendorId(),
                userPrincipal.getEmail(), null, null, refreshExpirationMs);
    }

    private String buildToken(String userId, String vendorId, String email, 
                              String role, String department, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(userId)
                .claim("vendorId", vendorId)
                .claim("email", email)
                .claim("role", role)
                .claim("department", department)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserIdFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (Exception ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
        }
        return false;
    }

    public long getExpirationTime() {
        return jwtExpirationMs;
    }

    public long getRefreshExpirationTime() {
        return refreshExpirationMs;
    }
}
