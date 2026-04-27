package com.cloudvault.backend.dto;

/**
 * Request body for creating a share link.
 * If expiresInHours is null, the share link never expires.
 */
public record CreateShareRequest(
        Long expiresInHours
) {
}
