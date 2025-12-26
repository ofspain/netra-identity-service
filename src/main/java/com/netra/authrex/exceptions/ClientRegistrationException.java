package com.netra.authrex.exceptions;

import com.netra.commons.exceptions.BaseException;

public class ClientRegistrationException extends RuntimeException implements BaseException {
    private String message;

    public ClientRegistrationException(String message){
        super();
        this.message = message;
    }
}
