package com.codebrokers.vitalink.iam.interfaces.rest;

import com.codebrokers.vitalink.iam.application.AuthenticationService;
import com.codebrokers.vitalink.iam.infrastructure.JwtTokenIssuer;
import com.codebrokers.vitalink.shared.security.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationService authentication;
    private final JwtTokenIssuer tokens;
    private final IdentityContextReader contexts;

    public AuthController(AuthenticationService authentication, JwtTokenIssuer tokens, IdentityContextReader contexts) {
        this.authentication = authentication; this.tokens = tokens; this.contexts = contexts;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var account = authentication.authenticate(request.email(), request.password());
        var user = UserResponse.from(account, contexts.contextFor(new AccountIdentity(account.id(), account.role())));
        response.setHeader("Cache-Control", "no-store");
        return new LoginResponse(tokens.issue(account), "Bearer", tokens.ttlSeconds(), user);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AccountIdentity identity, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return UserResponse.from(authentication.current(identity.accountId()), contexts.contextFor(identity));
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
        @Override public String toString() { return "LoginResponse[redacted]"; }
    }
}
