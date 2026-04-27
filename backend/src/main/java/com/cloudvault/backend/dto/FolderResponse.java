package com.cloudvault.backend.dto;

import java.util.UUID;

/**
 * Response DTO for folder metadata.
 */
public record FolderResponse(
        UUID id,
        String name,
        UUID parentId,
        String parentName,
        String createdAt
) {
}
