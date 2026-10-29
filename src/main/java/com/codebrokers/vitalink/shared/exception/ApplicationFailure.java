package com.codebrokers.vitalink.shared.exception;

public class ApplicationFailure extends RuntimeException {
    private final String code;

    public ApplicationFailure(String code) {
        super(code);
        this.code = code;
    }

    public String code() { return code; }
}
