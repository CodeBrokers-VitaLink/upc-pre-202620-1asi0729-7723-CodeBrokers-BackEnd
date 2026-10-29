package com.codebrokers.vitalink.shared.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@ConfigurationProperties("iam.jwt")
public record JwtProperties(String secretBase64, String issuer, String audience, long ttlSeconds) {
    public JwtProperties {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(secretBase64 == null ? "" : secretBase64); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("JWT secret must be valid Base64"); }
        if (bytes.length < 32) throw new IllegalArgumentException("JWT secret must contain at least 256 bits");
        if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank()
                || ttlSeconds < 60 || ttlSeconds > 3600) throw new IllegalArgumentException("Invalid JWT settings");
    }

    public SecretKey key() { return new SecretKeySpec(Base64.getDecoder().decode(secretBase64), "HmacSHA256"); }

    @Override public String toString() { return "JwtProperties[redacted]"; }
}
