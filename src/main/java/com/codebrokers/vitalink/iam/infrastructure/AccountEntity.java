package com.codebrokers.vitalink.iam.infrastructure;

import com.codebrokers.vitalink.iam.domain.UserAccount;
import com.codebrokers.vitalink.iam.domain.UserRole;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "iam_accounts")
public class AccountEntity {
    @Id @Column(length = 36) private String id;
    @Column(nullable = false, length = 254, unique = true) private String email;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Column(nullable = false, length = 120) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private UserRole role;
    @Column(nullable = false) private boolean active;
    @Column(length = 30) private String phone;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected AccountEntity() { }

    AccountEntity(UserAccount account) {
        id = account.id(); email = account.email(); passwordHash = account.passwordHash();
        name = account.name(); role = account.role(); active = account.active(); phone = account.phone();
        createdAt = account.createdAt(); updatedAt = account.updatedAt();
    }

    UserAccount toDomain() {
        return new UserAccount(id, email, passwordHash, name, role, active, phone, createdAt, updatedAt);
    }
}
