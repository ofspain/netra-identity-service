package com.netra.authrex.dtos;


public abstract class AuthRequest {

    public abstract AUTHGRANTTYPE getGrantType();

    public enum AUTHGRANTTYPE {
        PASSWORD("password"),
        CLIENT_CREDENTIALS("client_credentials"),
        TOKEN_EXCHANGE("urn:ietf:params:oauth:grant-type:token-exchange");

        private final String value;

        AUTHGRANTTYPE(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}

