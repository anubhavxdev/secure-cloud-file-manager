package com.cloudvault.backend.exception;

/**
 * Thrown when a Supabase Storage operation fails (upload, download, delete).
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
