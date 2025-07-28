package com.netra.authrex.exceptions;

public class AppDataAccessException extends RuntimeException {

    private final String errorCode;

    public AppDataAccessException(String message) {
        super(message);
        this.errorCode = null;
    }

    public AppDataAccessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public AppDataAccessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

