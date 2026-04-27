package com.cloudvault.backend.dto;

import java.util.UUID;

/**
 * Response DTO for file metadata (used in list, upload response, get metadata).
 */
public record FileMetadataResponse(
        UUID id,
        String originalName,
        String contentType,
        long size,
        UUID folderId,
        String folderName,
        String createdAt,
        String updatedAt
) {
}
