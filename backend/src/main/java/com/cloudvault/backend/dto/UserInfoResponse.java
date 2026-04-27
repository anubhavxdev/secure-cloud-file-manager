package com.cloudvault.backend.dto;

import java.util.UUID;

/**
 * Response body for the GET /api/auth/me endpoint.
 */
public record UserInfoResponse(
        UUID id,
        String email,
        String name,
        String role,
        String createdAt
) {
}
