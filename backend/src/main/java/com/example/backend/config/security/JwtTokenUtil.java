package com.example.backend.config.security;

import com.example.backend.domain.dto.UserLoginRequest;
import com.example.backend.domain.entity.User;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import org.slf4j.Logger;

import static java.lang.String.format;

@Component
public class JwtTokenUtil implements Serializable {

    // Expired time 30 mins
    public static final long JWT_TOKEN_VALIDITY = 30 * 60;

    private final Logger logger;

    public JwtTokenUtil(Logger logger) {
        this.logger = logger;
    }

    @Value("${jwt.secret}")
    private String jwtSecret;

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject().split(",")[0];
    }

    public String getTokenId(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
        return claims.getId();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();

        System.out.println("claims: ---> " + claims);

        return claims.getSubject().split(",")[1];
    }


    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
        return claims.getExpiration();
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
    }

    public String generateToken(User user, UserLoginRequest request) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .setId(id)
                .setClaims(claims)
                .setSubject(format("%s,%s", user.getId(), user.getUsername()))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature ---> {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token ---> {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token ---> {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token ---> {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty ---> {}", ex.getMessage());
        }
        return false;
    }

}