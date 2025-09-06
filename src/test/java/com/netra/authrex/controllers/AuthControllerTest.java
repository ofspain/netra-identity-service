package com.netra.authrex.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netra.authrex.dtos.AuthRequest;
import com.netra.authrex.dtos.RefreshTokenRequest;
import com.netra.authrex.services.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthRequest validAuthRequest;
    private AuthRequest invalidAuthRequest;

    @BeforeEach
    void setup() {
        // These should match a real user your AuthenticationService can validate.
        validAuthRequest = new AuthRequest("johndoe", "password123");
        invalidAuthRequest = new AuthRequest("johndoe", "wrongpassword");
    }

    @Test
    void testLogin_success() throws Exception {

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Authentication successful"))
                .andExpect(jsonPath("$.data.access_token").exists())
                .andExpect(jsonPath("$.data.refresh_token").exists())
                .andReturn();


        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Response JSON: " + responseBody);
    }

    @Test
    void testLogin_invalidCredentials() throws Exception {
        // when + then: Spring Security should translate UsernameNotFoundException into 401
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAuthRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Response: " + responseBody);
                //.andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void testRefreshToken_success() throws Exception {
        // First, login to get a real refresh token
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract refreshToken (pseudo-code: depends on your JSON structure)
        String refreshToken = objectMapper.readTree(loginResponse)
                .path("data")
                .path("refreshToken")
                .asText();

        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void testRefreshToken_expiredOrInvalid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("some-invalid-or-expired-token");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token expired")); // or "Invalid token"
    }

    @Test
    void testLogout_success() throws Exception {
        // Login to get a real refresh token
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper.readTree(loginResponse)
                .path("data")
                .path("refreshToken")
                .asText();

        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void testLogout_invalidToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid token"));
    }
}
