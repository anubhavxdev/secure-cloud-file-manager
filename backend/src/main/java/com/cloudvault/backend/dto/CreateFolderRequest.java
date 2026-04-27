package com.cloudvault.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request body for creating a new folder.
 */
public record CreateFolderRequest(

        @NotBlank(message = "Folder name is required")
        @Size(max = 255, message = "Folder name must not exceed 255 characters")
        String name,

        UUID parentId
) {
}
