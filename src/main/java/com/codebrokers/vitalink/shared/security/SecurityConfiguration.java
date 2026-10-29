package com.codebrokers.vitalink.shared.security;

import com.codebrokers.vitalink.iam.infrastructure.AccountJwtConverter;
import com.codebrokers.vitalink.shared.configuration.JwtProperties;
import com.codebrokers.vitalink.shared.exception.ApiErrors;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.cors.*;
import java.net.URI;
import java.time.Duration;
import java.util.List;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfiguration {
    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(properties.key()));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        var decoder = NimbusJwtDecoder.withSecretKey(properties.key()).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> requiredClaims = jwt -> {
            boolean valid = jwt.getExpiresAt() != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()
                    && jwt.getAudience() != null && jwt.getAudience().contains(properties.audience())
                    && ("PHYSICIAN".equals(jwt.getClaimAsString("role")) || "FAMILY_MEMBER".equals(jwt.getClaimAsString("role")));
            return valid ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ZERO),
                new JwtIssuerValidator(properties.issuer()), requiredClaims));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountJwtConverter converter, ApiErrors errors,
                                            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, ex) -> {
            String code = ex instanceof OAuth2AuthenticationException oauth
                    && oauth.getError().getErrorCode().equals("temporarily_unavailable")
                    ? "SERVICE_UNAVAILABLE" : "UNAUTHENTICATED";
            errors.write(code, request, response);
        };
        return http.cors(c -> c.configurationSource(cors))
                // Only explicit Authorization headers authenticate; no cookies or browser-managed credentials.
                .csrf(c -> c.disable())
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(c -> c.disable()).formLogin(c -> c.disable()).httpBasic(c -> c.disable()).logout(c -> c.disable())
                .authorizeHttpRequests(c -> c.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/**").authenticated().anyRequest().denyAll())
                .exceptionHandling(c -> c.authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler((request, response, ex) -> errors.write("ACCESS_DENIED", request, response)))
                .oauth2ResourceServer(c -> c.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler((request, response, ex) -> errors.write("ACCESS_DENIED", request, response)))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${iam.cors.allowed-origins}") List<String> origins) {
        for (String origin : origins) {
            URI uri = URI.create(origin);
            if (uri.getHost() == null || !List.of("http", "https").contains(uri.getScheme())
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || !uri.getPath().isEmpty()) throw new IllegalArgumentException("CORS requires explicit origins");
        }
        var config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Accept-Language"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", config);
        return source;
    }
}
