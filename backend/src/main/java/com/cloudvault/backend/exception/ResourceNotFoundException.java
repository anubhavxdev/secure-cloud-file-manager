package com.cloudvault.backend.exception;

/**
 * Thrown when a requested resource (file, folder, user) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(resourceType + " not found with identifier: " + identifier);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
