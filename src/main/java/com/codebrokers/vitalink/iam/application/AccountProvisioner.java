package com.codebrokers.vitalink.iam.application;

import com.codebrokers.vitalink.iam.domain.*;
import com.codebrokers.vitalink.shared.exception.ApplicationFailure;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
public class AccountProvisioner {
    private final AccountRepository accounts;
    private final PasswordEncoder passwords;

    public AccountProvisioner(AccountRepository accounts, PasswordEncoder passwords) {
        this.accounts = accounts;
        this.passwords = passwords;
    }

    @Transactional
    public UserAccount provision(String email, String password, String name, UserRole role) {
        if (password == null || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must contain at least 12 characters and at most 72 UTF-8 bytes");
        }
        String normalized = UserAccount.normalizeEmail(email);
        if (accounts.findByEmail(normalized).isPresent()) throw new ApplicationFailure("EMAIL_ALREADY_EXISTS");
        Instant now = Instant.now();
        UserAccount account = new UserAccount(UUID.randomUUID().toString(), normalized, passwords.encode(password),
                name, role, true, null, now, now);
        accounts.insert(account);
        return account;
    }
}
