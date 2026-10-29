package com.codebrokers.vitalink.iam.domain;

public enum UserRole {
    PHYSICIAN("doctor"), FAMILY_MEMBER("family");

    private final String apiValue;

    UserRole(String apiValue) { this.apiValue = apiValue; }

    public String apiValue() { return apiValue; }
}
