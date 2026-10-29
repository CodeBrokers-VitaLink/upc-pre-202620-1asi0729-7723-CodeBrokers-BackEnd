package com.codebrokers.vitalink;

import com.codebrokers.vitalink.iam.application.AccountProvisioner;
import com.codebrokers.vitalink.iam.domain.*;
import com.codebrokers.vitalink.shared.exception.ApplicationFailure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
class MySqlPersistenceTests {
    @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired AccountProvisioner provisioner;
    @Autowired AccountRepository accounts;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;

    @Test
    void flywayMigrationMatchesJpaAndPersistsAccounts() {
        var account = provisioner.provision(" MySql@Example.test ", "mysql test password", "MySQL", UserRole.PHYSICIAN);
        assertThat(accounts.findById(account.id()).orElseThrow().email()).isEqualTo("mysql@example.test");
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success=true", Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentNormalizedEmailInsertProducesOneConflictAtDatabaseBoundary() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<String> insert = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                try {
                    new TransactionTemplate(transactions).executeWithoutResult(status -> {
                        Instant now = Instant.now();
                        accounts.insert(new UserAccount(UUID.randomUUID().toString(), " Race@Example.test ",
                                "$2a$12$testhash", "Concurrent", UserRole.FAMILY_MEMBER, true, null, now, now));
                    });
                    return "CREATED";
                } catch (ApplicationFailure failure) { return failure.code(); }
            };
            var first = executor.submit(insert);
            var second = executor.submit(insert);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("CREATED", "EMAIL_ALREADY_EXISTS");
        }
        assertThat(jdbc.queryForObject("select count(*) from iam_accounts where email='race@example.test'", Integer.class)).isEqualTo(1);
    }
}
