package com.netra.authrex.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordAuthRequest extends AuthRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    public PasswordAuthRequest(){}
    public PasswordAuthRequest(String username, String password){
        this.username = username;
        this.password = password;
    }

    @Override
    public AUTHGRANTTYPE getGrantType() {
        return AUTHGRANTTYPE.PASSWORD;
    }
}

