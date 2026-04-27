package com.cloudvault.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class for generating and validating JWT tokens.
 * Uses HMAC-SHA256 signing with a Base64-encoded secret key.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    /**
     * Generate a JWT token for the given email (subject).
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extract the email (subject) from a JWT token.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract the expiration date from a JWT token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Validate a JWT token: checks signature, expiration, and well-formedness.
     * Returns true if the token is valid, false otherwise.
     */
    public boolean validateToken(String token) {
        try {
            parseAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException e) {
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validate that the token is valid AND belongs to the given email.
     */
    public boolean validateToken(String token, String email) {
        try {
            String subject = extractEmail(token);
            return subject.equals(email) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = parseAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims parseAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
