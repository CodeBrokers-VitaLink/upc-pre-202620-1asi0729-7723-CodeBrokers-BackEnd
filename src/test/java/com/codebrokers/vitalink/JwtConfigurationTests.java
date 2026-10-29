package com.codebrokers.vitalink;

import com.codebrokers.vitalink.shared.configuration.JwtProperties;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;

class JwtConfigurationTests {
    @Test
    void rejectsMissingMalformedAndShortSecretsAndUnsafeLifetime() {
        for (String secret : new String[] { null, "", "not-base64!", Base64.getEncoder().encodeToString(new byte[31]) }) {
            assertThatThrownBy(() -> new JwtProperties(secret, "issuer", "audience", 900)).isInstanceOf(IllegalArgumentException.class);
        }
        String secret = Base64.getEncoder().encodeToString(new byte[32]);
        assertThatThrownBy(() -> new JwtProperties(secret, "issuer", "audience", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties(secret, "issuer", "audience", 86400)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new JwtProperties(secret, "issuer", "audience", 900).toString()).doesNotContain(secret);
    }
}
