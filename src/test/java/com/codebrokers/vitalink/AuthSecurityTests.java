package com.codebrokers.vitalink;

import com.codebrokers.vitalink.iam.application.AccountProvisioner;
import com.codebrokers.vitalink.iam.domain.*;
import com.codebrokers.vitalink.shared.configuration.JwtProperties;
import com.codebrokers.vitalink.shared.exception.ApplicationFailure;
import com.codebrokers.vitalink.shared.security.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthSecurityTests.PolicyProbe.class)
class AuthSecurityTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AccountProvisioner provisioner;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtProperties jwt;
    @MockitoBean PatientAccessReader access;
    @MockitoBean IdentityContextReader contexts;
    UserAccount doctor;
    UserAccount family;
    static final String PASSWORD = "test password 123";

    @BeforeEach
    void setUp() {
        jdbc.update("delete from iam_accounts");
        doctor = provisioner.provision("doctor@example.test", PASSWORD, "Doctor", UserRole.PHYSICIAN);
        family = provisioner.provision("family@example.test", PASSWORD, "Family", UserRole.FAMILY_MEMBER);
        when(contexts.contextFor(any())).thenReturn(CareContext.empty());
        when(access.authorizedPatientIds(any())).thenAnswer(call -> {
            AccountIdentity identity = call.getArgument(0);
            return identity.accountId().equals(doctor.id()) ? Set.of("patient-a") : Set.of("patient-b");
        });
    }

    @Test
    void loginAndMeUseServerIdentityAndExposeNoCredentials() throws Exception {
        for (var account : List.of(doctor, family)) {
            String token = login(account.email());
            var parsed = SignedJWT.parse(token);
            assertThat(parsed.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
            assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo(account.id());
            assertThat(parsed.getJWTClaimsSet().getStringClaim("role")).isEqualTo(account.role().name());
            mvc.perform(get("/api/v1/auth/me").param("id", "another-account").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(account.id()))
                    .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist())
                    .andExpect(cookie().doesNotExist("JSESSIONID"));
        }
    }

    @Test
    void invalidCredentialsDoNotEnumerateAccounts() throws Exception {
        jdbc.update("update iam_accounts set active=false where id=?", family.id());
        for (String email : List.of(doctor.email(), "missing@example.test", family.email())) {
            mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                            .content(mapper.writeValueAsString(Map.of("email", email, "password", "wrong password"))))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of("email", family.email(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void invalidAndUnknownFieldsAreRejectedWithoutEchoingPassword() throws Exception {
        for (String body : List.of("{", "{}", "{\"email\":\"bad\",\"password\":\"sensitive\"}",
                "{\"email\":\"doctor@example.test\",\"password\":\"sensitive\",\"role\":\"PHYSICIAN\"}")) {
            String result = mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(result).doesNotContain("sensitive");
        }
    }

    @Test
    void missingTokenAndPublicRegistrationAreNotAllowed() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(post("/api/v1/auth/register")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "future", "issuer", "audience", "subject", "expiration", "role", "missing-role", "missing-audience", "missing-subject", "signature", "algorithm", "none", "malformed"})
    void invalidJwtIsRejectedByRealDecoder(String variant) throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token(variant)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void existingTokenRespectsAccountDeactivationAndRoleChange() throws Exception {
        String token = login(doctor.email());
        jdbc.update("update iam_accounts set active=false where id=?", doctor.id());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        jdbc.update("update iam_accounts set active=true, role='FAMILY_MEMBER' where id=?", doctor.id());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void policyUsesLiveRelationshipsAndDeniesFamilyWrites() throws Exception {
        String physician = login(doctor.email());
        String relative = login(family.email());
        mvc.perform(get("/api/v1/test-policy/patient-a").header("Authorization", "Bearer " + physician)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/test-policy/patient-b").header("Authorization", "Bearer " + physician))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PATIENT_ACCESS_DENIED"));
        mvc.perform(get("/api/v1/test-policy/patient-b").header("Authorization", "Bearer " + relative)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/test-policy/patient-a").header("Authorization", "Bearer " + relative)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/test-policy/patient-b").header("Authorization", "Bearer " + relative))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        doReturn(Set.of()).when(access).authorizedPatientIds(any());
        mvc.perform(get("/api/v1/test-policy/patient-a").header("Authorization", "Bearer " + physician)).andExpect(status().isForbidden());
        doThrow(new ApplicationFailure("AFFILIATIONS_UNAVAILABLE")).when(access).authorizedPatientIds(any());
        mvc.perform(get("/api/v1/test-policy/patient-a").header("Authorization", "Bearer " + physician))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("AFFILIATIONS_UNAVAILABLE"));
    }

    @Test
    void corsAndLocalizedFilterErrorsAreStateless() throws Exception {
        mvc.perform(options("/api/v1/auth/me").header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        mvc.perform(options("/api/v1/auth/me").header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(get("/api/v1/auth/me").header("Accept-Language", "es-PE"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Se requiere autenticación."));
        mvc.perform(get("/api/v1/auth/me")).andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    private String login(String email) throws Exception {
        var response = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of("email", " " + email.toUpperCase(Locale.ROOT) + " ", "password", PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900)).andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("accessToken").asText();
    }

    private String token(String variant) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer(variant.equals("issuer") ? "wrong" : jwt.issuer())
                .audience(variant.equals("missing-audience") ? null : variant.equals("audience") ? "wrong" : jwt.audience())
                .subject(variant.equals("missing-subject") ? null : variant.equals("subject") ? "" : doctor.id())
                .claim("role", variant.equals("missing-role") ? null : variant.equals("role") ? "ADMIN" : "PHYSICIAN")
                .notBeforeTime(Date.from(now.plusSeconds(variant.equals("future") ? 600 : -60)));
        if (!variant.equals("expiration")) claims.expirationTime(Date.from(now.plusSeconds(variant.equals("expired") ? -10 : 600)));
        if (variant.equals("none")) return new PlainJWT(claims.build()).serialize();
        if (variant.equals("malformed")) return "invalid.jwt.value";
        boolean otherAlgorithm = variant.equals("algorithm");
        var signed = new SignedJWT(new JWSHeader(otherAlgorithm ? JWSAlgorithm.HS384 : JWSAlgorithm.HS256), claims.build());
        byte[] secret = otherAlgorithm ? new byte[48] : variant.equals("signature") ? new byte[32] : jwt.key().getEncoded();
        signed.sign(new MACSigner(secret));
        return signed.serialize();
    }

    // Test-only endpoint exercises the real filter chain and application policy, not clinical integration.
    @RestController
    static class PolicyProbe {
        private final PatientAccessPolicy policy;
        PolicyProbe(PatientAccessPolicy policy) { this.policy = policy; }
        @GetMapping("/api/v1/test-policy/{patientId}")
        Map<String, String> read(@AuthenticationPrincipal AccountIdentity identity, @PathVariable String patientId) {
            policy.requireRead(identity, patientId); return Map.of("id", patientId);
        }
        @PostMapping("/api/v1/test-policy/{patientId}")
        void write(@AuthenticationPrincipal AccountIdentity identity, @PathVariable String patientId) {
            policy.requireClinicalWrite(identity, patientId);
        }
    }
}
