package com.netra.authrex.configs;

import com.netra.authrex.dtos.AuthUser;
import com.netra.commons.models.Identity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtConfig {

    @Value("${app.jwt.private-key}")
    private Resource privateKeyResource;

    @Value("${app.jwt.public-key}")
    private Resource publicKeyResource;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.jwt.issuer}")
    private String issuer;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public Resource getPublicKeyResource(){
        return publicKeyResource;
    }


    private PrivateKey getPrivateKey(){
        try{
            if (privateKey == null) {
                try (InputStream is = privateKeyResource.getInputStream()) {
                    byte[] keyBytes = is.readAllBytes();
                    String keyPem = new String(keyBytes)
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s+", "");
                    byte[] decoded = java.util.Base64.getDecoder().decode(keyPem);
                    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
                    privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
                }
            }
            return privateKey;
        }catch (Exception exp){
            throw new RuntimeException("Unable to get private jwt key");
        }
    }

    private PublicKey getPublicKey(){
        try{
            if (publicKey == null) {
                try (InputStream is = publicKeyResource.getInputStream()) {
                    byte[] keyBytes = is.readAllBytes();
                    String keyPem = new String(keyBytes)
                        .replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s+", "");
                    byte[] decoded = java.util.Base64.getDecoder().decode(keyPem);
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
                    publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
                }
            }
            return publicKey;
        }catch(Exception exp){
            throw new RuntimeException("unable to get public jwt key");
        }
    }


    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(AuthUser authUser){
        return generateToken(new HashMap<>(), authUser);
    }

    public String generateToken(Map<String, Object> extraClaims, AuthUser authUser) {
        return buildToken(extraClaims, authUser, jwtExpiration);
    }

//    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
//        return Jwts.builder()
//                .setClaims(extraClaims)
//                .setSubject(userDetails.getUsername())
//                .setIssuer(issuer)
//                .setIssuedAt(new Date(System.currentTimeMillis()))
//                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
//                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
//                .compact();
//    }

    private String buildToken(Map<String, Object> extraClaims, AuthUser authUser, long expiration) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(authUser.getUsername())
                .setIssuer(issuer)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000));

        Identity identity = authUser.getIdentity();
        String domainCode = identity.getDomainCode();
        String domainType = identity.getDomainType().name();
        String identityUUID = identity.getIdentityUuid();
        LocalDateTime lastLogin = identity.getLastLogin();
        LocalDateTime lastPasswordChange = identity.getPasswordLastChanged();

        // Ensure map exists
        extraClaims = (extraClaims == null ? new HashMap<>() : extraClaims);

        // ✅ Put application-specific details into the claims
        extraClaims.put("domain_code", domainCode);
        extraClaims.put("domain_type", domainType);
        extraClaims.put("identity_uuid", identityUUID);
        if (lastLogin != null) {
            extraClaims.put("last_login", lastLogin.toString());
        }
        if (lastPasswordChange != null) {
            extraClaims.put("last_password_change", lastPasswordChange.toString());
        }

        // Merge extra claims into builder
        for (Map.Entry<String, Object> entry : extraClaims.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }

        // Always add roles from UserDetails
        List<String> roles = authUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        builder.claim("roles", roles);

        return builder
                .signWith(getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }



    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .setSigningKey(getPublicKey()) // verify with public key
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}
