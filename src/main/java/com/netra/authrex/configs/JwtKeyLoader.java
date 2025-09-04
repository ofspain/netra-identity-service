package com.netra.authrex.configs;

import com.netra.authrex.configs.utils.JwtProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class JwtKeyLoader {

    private final JwtProperties jwtProperties;

    public JwtKeyLoader(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String getPrivateKeyContent() throws Exception {
        try (var reader = new InputStreamReader(jwtProperties.getPrivateKey().getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    public String getPublicKeyContent() throws Exception {
        try (var reader = new InputStreamReader(jwtProperties.getPublicKey().getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }
}

