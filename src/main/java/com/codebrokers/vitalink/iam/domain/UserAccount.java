package com.codebrokers.vitalink.iam.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public final class UserAccount {
    private final String id;
    private final String email;
    private final String passwordHash;
    private final String name;
    private final UserRole role;
    private final boolean active;
    private final String phone;
    private final Instant createdAt;
    private final Instant updatedAt;

    public UserAccount(String id, String email, String passwordHash, String name, UserRole role,
                       boolean active, String phone, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.email = normalizeEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.name = Objects.requireNonNull(name).trim();
        this.role = Objects.requireNonNull(role);
        this.active = active;
        this.phone = phone;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        if (id.isBlank() || id.length() > 36 || this.email.length() > 254
                || !this.email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || this.name.isBlank() || this.name.length() > 120
                || passwordHash.isBlank() || passwordHash.length() > 100
                || (phone != null && phone.length() > 30)) {
            throw new IllegalArgumentException("Invalid account fields");
        }
    }

    public static String normalizeEmail(String email) {
        return Objects.requireNonNull(email).trim().toLowerCase(Locale.ROOT);
    }

    public String id() { return id; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public String name() { return name; }
    public UserRole role() { return role; }
    public boolean active() { return active; }
    public String phone() { return phone; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
