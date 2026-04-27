package com.cloudvault.backend.dto;

/**
 * Response body returned after successful authentication (login or register).
 */
public record AuthResponse(
        String token,
        String email,
        String name,
        String role
) {
}
