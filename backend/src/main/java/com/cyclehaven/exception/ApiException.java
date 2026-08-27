package com.cyclehaven.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for errors that map to a specific HTTP status.
 *
 * <p>Throwing these from a service keeps controllers free of status-code plumbing;
 * {@link GlobalExceptionHandler} turns them into a consistent JSON body.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
