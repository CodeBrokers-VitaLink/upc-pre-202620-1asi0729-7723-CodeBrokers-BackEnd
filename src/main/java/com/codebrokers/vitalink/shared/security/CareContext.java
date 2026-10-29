package com.codebrokers.vitalink.shared.security;

public record CareContext(String providerId, String familyId, String clinicId) {
    public static CareContext empty() { return new CareContext(null, null, null); }
}
