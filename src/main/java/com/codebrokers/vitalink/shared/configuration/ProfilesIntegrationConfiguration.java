package com.codebrokers.vitalink.shared.configuration;

import com.codebrokers.vitalink.shared.exception.ApplicationFailure;
import com.codebrokers.vitalink.shared.security.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProfilesIntegrationConfiguration {
    @Bean
    @ConditionalOnMissingBean(IdentityContextReader.class)
    IdentityContextReader unlinkedIdentityContext() {
        // Profiles is not implemented yet. This supplies no clinical identity or permission.
        return identity -> CareContext.empty();
    }

    @Bean
    @ConditionalOnMissingBean(PatientAccessReader.class)
    PatientAccessReader unavailablePatientAccess() {
        return identity -> { throw new ApplicationFailure("AFFILIATIONS_UNAVAILABLE"); };
    }
}
