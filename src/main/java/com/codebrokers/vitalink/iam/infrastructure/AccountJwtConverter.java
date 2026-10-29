package com.codebrokers.vitalink.iam.infrastructure;

import com.codebrokers.vitalink.iam.domain.AccountRepository;
import com.codebrokers.vitalink.shared.security.AccountIdentity;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AccountJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final AccountRepository accounts;

    public AccountJwtConverter(AccountRepository accounts) { this.accounts = accounts; }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            var account = accounts.findById(jwt.getSubject()).orElseThrow(() -> invalid("invalid_token"));
            if (!account.active() || !account.role().name().equals(jwt.getClaimAsString("role"))) {
                throw invalid("invalid_token");
            }
            var identity = new AccountIdentity(account.id(), account.role());
            return UsernamePasswordAuthenticationToken.authenticated(identity, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name())));
        } catch (DataAccessException ex) {
            throw invalid("temporarily_unavailable");
        }
    }

    private OAuth2AuthenticationException invalid(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code));
    }
}
