package com.netra.authrex.configs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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
import java.util.Date;
import java.util.HashMap;
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

    public String generateToken(UserDetails userDetails){
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
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

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuer(issuer)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
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
