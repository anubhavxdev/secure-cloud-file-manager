package com.cloudvault.backend.dto;

import java.util.List;

/**
 * Response DTO for folder contents — subfolders and files within a folder.
 */
public record FolderContentsResponse(
        FolderResponse folder,
        List<FolderResponse> subfolders,
        List<FileMetadataResponse> files
) {
}
