package com.codebrokers.vitalink.shared.security;

public interface IdentityContextReader {
    CareContext contextFor(AccountIdentity identity);
}
