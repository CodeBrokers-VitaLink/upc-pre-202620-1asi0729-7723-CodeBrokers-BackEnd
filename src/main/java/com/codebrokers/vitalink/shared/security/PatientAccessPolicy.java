package com.codebrokers.vitalink.shared.security;

import com.codebrokers.vitalink.iam.domain.UserRole;
import com.codebrokers.vitalink.shared.exception.ApplicationFailure;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class PatientAccessPolicy {
    private final PatientAccessReader reader;

    public PatientAccessPolicy(PatientAccessReader reader) { this.reader = reader; }

    public Set<String> scope(AccountIdentity identity) {
        if (identity == null) throw new ApplicationFailure("UNAUTHENTICATED");
        return Set.copyOf(reader.authorizedPatientIds(identity));
    }

    public void requireRead(AccountIdentity identity, String patientId) {
        if (!scope(identity).contains(patientId)) throw new ApplicationFailure("PATIENT_ACCESS_DENIED");
    }

    public void requireClinicalWrite(AccountIdentity identity, String patientId) {
        if (identity == null) throw new ApplicationFailure("UNAUTHENTICATED");
        if (identity.role() != UserRole.PHYSICIAN) throw new ApplicationFailure("ACCESS_DENIED");
        requireRead(identity, patientId);
    }
}
