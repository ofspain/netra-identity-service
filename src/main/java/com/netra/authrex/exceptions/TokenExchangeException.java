package com.netra.authrex.exceptions;

import com.netra.commons.exceptions.BaseException;

public class TokenExchangeException  extends RuntimeException implements BaseException {
    private String message;

    public TokenExchangeException(String message){
        super();
        this.message = message;
    }
}
