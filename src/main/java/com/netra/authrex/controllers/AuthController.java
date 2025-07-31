package com.netra.authrex.controllers;

import com.netra.authrex.dtos.AuthApiResponse;
import com.netra.authrex.dtos.AuthRequest;
import com.netra.authrex.dtos.AuthResponse;
import com.netra.authrex.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate a user and get a JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthApiResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = authenticationService.authenticate(request);
        return ResponseEntity.ok(AuthApiResponse.success(
                response,
                "Authentication successful"
        ));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate Token", description = "Check if a JWT token is valid")
    public ResponseEntity<AuthApiResponse> validate(@RequestParam String token) {
        boolean valid = authenticationService.validateToken(token);
        return ResponseEntity.ok(AuthApiResponse.success(
                valid,
                "Validation successful"
        ));
    }
}