package com.codebrokers.vitalink.iam.infrastructure;

import com.codebrokers.vitalink.iam.application.AccountProvisioner;
import com.codebrokers.vitalink.iam.domain.*;
import com.codebrokers.vitalink.shared.exception.ApplicationFailure;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("local & !prod")
@ConditionalOnProperty(name = "iam.bootstrap.enabled", havingValue = "true")
public class LocalAccountBootstrap implements ApplicationRunner {
    private final AccountRepository accounts;
    private final AccountProvisioner provisioner;
    private final Environment environment;

    public LocalAccountBootstrap(AccountRepository accounts, AccountProvisioner provisioner, Environment environment) {
        this.accounts = accounts; this.provisioner = provisioner; this.environment = environment;
    }

    public void run(ApplicationArguments args) {
        create("doctor@example.test", "Demo Doctor", UserRole.PHYSICIAN, "LOCAL_DOCTOR_PASSWORD");
        create("family@example.test", "Demo Family", UserRole.FAMILY_MEMBER, "LOCAL_FAMILY_PASSWORD");
    }

    private void create(String email, String name, UserRole role, String passwordProperty) {
        if (accounts.findByEmail(email).isPresent()) return;
        String password = environment.getRequiredProperty(passwordProperty);
        try {
            provisioner.provision(email, password, name, role);
        } catch (ApplicationFailure ex) {
            if (!ex.code().equals("EMAIL_ALREADY_EXISTS")) throw ex;
        }
    }
}
