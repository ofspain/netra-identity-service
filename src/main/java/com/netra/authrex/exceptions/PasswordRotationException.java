package com.netra.authrex.exceptions;

import com.netra.commons.exceptions.BaseException;

public class PasswordRotationException extends RuntimeException implements BaseException {

    private String message;

    public PasswordRotationException(String message){
        super();
        this.message = message;
    }
}
