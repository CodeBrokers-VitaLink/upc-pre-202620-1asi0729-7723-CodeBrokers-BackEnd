package com.codebrokers.vitalink.iam.infrastructure;

import com.codebrokers.vitalink.iam.domain.UserAccount;
import com.codebrokers.vitalink.shared.configuration.JwtProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;

@Component
public class JwtTokenIssuer {
    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtTokenIssuer(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder; this.properties = properties;
    }

    public String issue(UserAccount account) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(properties.issuer()).audience(List.of(properties.audience()))
                .subject(account.id()).issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(properties.ttlSeconds()))
                .claim("role", account.role().name()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public long ttlSeconds() { return properties.ttlSeconds(); }
}
