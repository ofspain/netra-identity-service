package com.netra.authrex.exceptions;

import com.netra.commons.exceptions.BaseException;

public class TokenRefreshException extends RuntimeException implements BaseException {
    private String message;

    public TokenRefreshException(String message){
        super();
        this.message = message;
    }
}
