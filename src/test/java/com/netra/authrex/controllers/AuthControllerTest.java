package com.netra.authrex.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netra.authrex.dtos.AuthApiResponse;
import com.netra.authrex.dtos.AuthRequest;
import com.netra.authrex.dtos.AuthResponse;
import com.netra.authrex.dtos.RefreshTokenRequest;
import com.netra.authrex.services.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthRequest authRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setup() {
        authRequest = new AuthRequest("johndoe", "password123");
        authResponse = new AuthResponse("jwt-token", 3000l, );
    }

    @Test
    void testLogin_success() throws Exception {
        Mockito.when(authenticationService.authenticate(any(AuthRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Authentication successful"))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    void testRefreshToken_success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        Mockito.when(authenticationService.refreshToken("refresh-token"))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .header("X-Trace-Id", "trace-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"));
    }

    @Test
    void testLogout_success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        doNothing().when(authenticationService).logout("refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                        .header("X-Trace-Id", "trace-456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void testLogin_invalidCredentials() throws Exception {
        Mockito.when(authenticationService.authenticate(any(AuthRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void testRefreshToken_expiredToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("expired-token");

        Mockito.when(authenticationService.refreshToken("expired-token"))
                .thenThrow(new RuntimeException("Refresh token expired"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token expired"));
    }

    @Test
    void testLogout_invalidToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

        Mockito.doThrow(new RuntimeException("Invalid token"))
                .when(authenticationService).logout("invalid-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid token"));
    }
}
