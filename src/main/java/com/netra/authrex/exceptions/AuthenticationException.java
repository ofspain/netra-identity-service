package com.netra.authrex.exceptions;

import com.netra.commons.exceptions.BaseException;

public class AuthenticationException  extends RuntimeException implements BaseException {

    private String message;

    public AuthenticationException(String message){
        super();
        this.message = message;
    }
}
