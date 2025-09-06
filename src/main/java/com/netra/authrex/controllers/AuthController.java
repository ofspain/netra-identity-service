package com.netra.authrex.controllers;

import com.netra.authrex.dtos.AuthApiResponse;
import com.netra.authrex.dtos.AuthRequest;
import com.netra.authrex.dtos.AuthResponse;
import com.netra.authrex.dtos.RefreshTokenRequest;
import com.netra.authrex.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

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
    public ResponseEntity<AuthApiResponse<AuthResponse>> login(
            @RequestBody AuthRequest request,
            HttpServletRequest httpRequest) {  // Leave it even if unused

        AuthResponse response = authenticationService.authenticate(request);
        return ResponseEntity.ok(AuthApiResponse.success(
                response,
                "Authentication successful"
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthApiResponse<AuthResponse>> refreshToken(  // Fixed generic type
                                                                        @RequestBody RefreshTokenRequest request,
                                                                        @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                                                        HttpServletRequest httpServletRequest) {  // Leave it even if unused

        AuthResponse authResponse = authenticationService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(AuthApiResponse.success(
                authResponse,
                "Token refreshed successfully"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthApiResponse<Void>> logout(
            @RequestBody RefreshTokenRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            HttpServletRequest httpServletRequest) {  // Added missing parameter

        authenticationService.logout(request.refreshToken());
        return ResponseEntity.ok(AuthApiResponse.success(
                null,
                "Logout successful"
        ));
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    @GetMapping(value = "/public-key", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getPublicKey(HttpServletRequest request) throws Exception {
        return authenticationService.retrievePublicKey();
    }
}

/**
 * GOING FORWARD ON CLIENTS
 *
 */
//We’ll fetch the public key once at startup (or periodically refresh it, e.g. every 6–12 hours).
//Options:
//
//Simple in-memory cache (good enough if key rotation is rare).
//
//Scheduled refresh (if you expect to rotate keys occasionally).
//
//2. Updated JwtVerifier with Cache
//package com.netra.client.security;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//
//import java.io.InputStream;
//import java.net.URL;
//import java.nio.charset.StandardCharsets;
//import java.security.KeyFactory;
//import java.security.PublicKey;
//import java.security.spec.X509EncodedKeySpec;
//import java.util.Base64;
//
//public class JwtVerifier {
//
//    private PublicKey publicKey;
//    private final String jwkUrl;
//
//    public JwtVerifier(String jwkUrl) throws Exception {
//        this.jwkUrl = jwkUrl;
//        this.publicKey = fetchPublicKey(); // fetch once at startup
//    }
//
//    private PublicKey fetchPublicKey() throws Exception {
//        try (InputStream in = new URL(jwkUrl).openStream()) {
//            String keyPem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
//            return parsePublicKey(keyPem);
//        }
//    }
//
//    private PublicKey parsePublicKey(String pem) throws Exception {
//        String clean = pem.replace("-----BEGIN PUBLIC KEY-----", "")
//                .replace("-----END PUBLIC KEY-----", "")
//                .replaceAll("\\s+", "");
//        byte[] decoded = Base64.getDecoder().decode(clean);
//        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
//        return KeyFactory.getInstance("RSA").generatePublic(spec);
//    }
//
//    public Claims validateAndParseClaims(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(publicKey)
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//    }
//
//    /**
//     * Call this if you want to refresh the cached key manually
//     */
//    public void refreshKey() throws Exception {
//        this.publicKey = fetchPublicKey();
//    }
//}
//
//3. Scheduled Refresh (Optional)
//
//If you rotate your keys periodically, you can refresh automatically:
//
//package com.netra.client.security;
//
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//public class PublicKeyRefresher {
//
//    private final JwtVerifier jwtVerifier;
//
//    public PublicKeyRefresher(JwtVerifier jwtVerifier) {
//        this.jwtVerifier = jwtVerifier;
//    }
//
//    // Refresh every 12 hours
//    @Scheduled(fixedDelay = 12 * 60 * 60 * 1000)
//    public void refreshPublicKey() {
//        try {
//            jwtVerifier.refreshKey();
//            System.out.println("🔄 Public key refreshed");
//        } catch (Exception e) {
//            System.err.println("❌ Failed to refresh public key: " + e.getMessage());
//        }
//    }
//}
//
//
//Don’t forget to enable scheduling in your client app:
//
//@SpringBootApplication
//@EnableScheduling
//public class ClientApplication { ... }
//
//4. JwtAuthFilter (same as before)
//
//The filter keeps working — now backed by the cached public key.
//
//✅ With this setup:
//
//First time the client service starts → it fetches the public key.
//
//Public key is cached in memory.
//
//Optionally, a background task refreshes it (handles key rotation).
//
//👉 Do you want me to also show you how to support JWKS (JSON Web Key Set) instead of plain .pub files? That’s the more standard way for publishing multiple public keys.