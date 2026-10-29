package com.codebrokers.vitalink;

import com.codebrokers.vitalink.iam.application.AccountProvisioner;
import com.codebrokers.vitalink.iam.domain.*;
import com.codebrokers.vitalink.shared.exception.ApplicationFailure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class AccountPersistenceTests {
    @Autowired AccountProvisioner provisioner;
    @Autowired AccountRepository accounts;
    @Autowired PasswordEncoder passwords;

    @Test
    void persistsNormalizedAccountWithAdaptiveHash() {
        var created = provisioner.provision("  Doctor@Example.test ", "long test password", "Doctor", UserRole.PHYSICIAN);
        var stored = accounts.findByEmail("doctor@example.test").orElseThrow();
        assertThat(stored.id()).isEqualTo(created.id());
        assertThat(stored.passwordHash()).startsWith("$2a$12$").isNotEqualTo("long test password");
        assertThat(passwords.matches("long test password", stored.passwordHash())).isTrue();
        assertThat(stored.active()).isTrue();
        assertThat(stored.toString()).doesNotContain(stored.passwordHash());
    }

    @Test
    void rejectsNormalizedDuplicatesAndOversizedUtf8Password() {
        provisioner.provision("family@example.test", "long test password", "Family", UserRole.FAMILY_MEMBER);
        assertThatThrownBy(() -> provisioner.provision(" FAMILY@example.test ", "other test password", "Other", UserRole.PHYSICIAN))
                .isInstanceOf(ApplicationFailure.class).hasMessage("EMAIL_ALREADY_EXISTS");
        assertThatThrownBy(() -> provisioner.provision("new@example.test", "é".repeat(37), "Other", UserRole.PHYSICIAN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
