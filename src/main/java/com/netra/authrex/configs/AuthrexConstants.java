package com.netra.authrex.configs;


public class AuthrexConstants {

    // Token types
    public static final String TOKEN_TYPE_SERVICE = "service";
    public static final String TOKEN_TYPE_DELEGATED = "delegated";
    public static final String TOKEN_TYPE_USER = "user";

    // Claim names
    public static final String CLAIM_SERVICE_PRINCIPAL = "is_service_principal";
    public static final String CLAIM_DELEGATED_TOKEN = "is_delegated_token";
    public static final String CLAIM_CLIENT_ID = "client_id";
    public static final String CLAIM_ORIGINAL_USER = "original_sub";
    public static final String CLAIM_ACTING_ON_BEHALF_OF = "acting_on_behalf_of";

    // Roles
    public static final String ROLE_SERVICE = "ROLE_SERVICE";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_DELEGATED = "ROLE_DELEGATED";
}
