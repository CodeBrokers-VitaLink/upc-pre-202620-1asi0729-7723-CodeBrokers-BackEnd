package com.codebrokers.vitalink.iam.application;

import com.codebrokers.vitalink.iam.domain.*;
import com.codebrokers.vitalink.shared.exception.ApplicationFailure;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class AuthenticationService {
    private final AccountRepository accounts;
    private final PasswordEncoder passwords;
    private final String dummyHash;

    public AuthenticationService(AccountRepository accounts, PasswordEncoder passwords) {
        this.accounts = accounts; this.passwords = passwords;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public UserAccount authenticate(String email, String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApplicationFailure("INVALID_CREDENTIALS");
        }
        var account = accounts.findByEmail(UserAccount.normalizeEmail(email));
        boolean matches = passwords.matches(password, account.map(UserAccount::passwordHash).orElse(dummyHash));
        if (!matches || account.isEmpty() || !account.get().active()) throw new ApplicationFailure("INVALID_CREDENTIALS");
        return account.get();
    }

    @Transactional(readOnly = true)
    public UserAccount current(String id) {
        return accounts.findById(id).filter(UserAccount::active).orElseThrow(() -> new ApplicationFailure("UNAUTHENTICATED"));
    }
}
