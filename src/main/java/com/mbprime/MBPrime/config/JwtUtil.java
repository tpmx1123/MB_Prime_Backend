package com.mbprime.MBPrime.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Simple token util (no jjwt dependency). Token format: base64(username:expiry:hmac).
 */
@Component
public class JwtUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    public String generateToken(String username) {
        long expiry = System.currentTimeMillis() + expirationMs;
        String payload = username + ":" + expiry;
        String hmac = hmac(payload);
        String token = payload + ":" + hmac;
        return Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    public String getUsernameFromToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 3);
            if (parts.length != 3) return null;
            String username = parts[0];
            long expiry = Long.parseLong(parts[1]);
            String expectedHmac = hmac(username + ":" + expiry);
            if (!expectedHmac.equals(parts[2])) return null;
            if (expiry < System.currentTimeMillis()) return null;
            return username;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token, String username) {
        String tokenUsername = getUsernameFromToken(token);
        return tokenUsername != null && tokenUsername.equals(username);
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC failed", e);
        }
    }
}
