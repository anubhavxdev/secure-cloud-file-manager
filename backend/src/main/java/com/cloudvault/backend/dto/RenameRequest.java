package com.cloudvault.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for renaming a file or folder.
 */
public record RenameRequest(

        @NotBlank(message = "New name is required")
        @Size(max = 500, message = "Name must not exceed 500 characters")
        String newName
) {
}
