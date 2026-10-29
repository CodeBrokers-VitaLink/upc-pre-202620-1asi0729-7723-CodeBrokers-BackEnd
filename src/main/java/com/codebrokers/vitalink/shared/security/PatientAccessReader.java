package com.codebrokers.vitalink.shared.security;

import java.util.Set;

public interface PatientAccessReader {
    // Return current persisted relationships; technical failures must never become empty or unrestricted scopes.
    Set<String> authorizedPatientIds(AccountIdentity identity);
}
