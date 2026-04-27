package com.cloudvault.backend.exception;

/**
 * Thrown when a user attempts to access a resource they do not own.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
