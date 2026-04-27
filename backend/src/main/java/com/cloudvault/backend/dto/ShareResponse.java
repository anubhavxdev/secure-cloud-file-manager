package com.cloudvault.backend.dto;

import java.util.UUID;

/**
 * Response DTO for a share token (returned when creating or listing shares).
 */
public record ShareResponse(
        UUID id,
        UUID fileId,
        String fileName,
        UUID token,
        String shareUrl,
        String expiresAt,
        String createdAt
) {
}
