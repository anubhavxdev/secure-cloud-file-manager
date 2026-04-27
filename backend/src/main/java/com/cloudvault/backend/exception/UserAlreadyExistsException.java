package com.cloudvault.backend.exception;

/**
 * Thrown when a registration attempt uses an email that already exists.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
