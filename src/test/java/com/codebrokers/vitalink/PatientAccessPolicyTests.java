package com.codebrokers.vitalink;

import com.codebrokers.vitalink.iam.domain.UserRole;
import com.codebrokers.vitalink.shared.exception.ApplicationFailure;
import com.codebrokers.vitalink.shared.security.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class PatientAccessPolicyTests {
    @Test
    void isolatesTwoClinicsAndCaregiverNetworksWithoutConfusingAccountAndProfileIds() {
        Map<String, Set<String>> persistedLinks = new HashMap<>(Map.of(
                "account-doctor-a", Set.of("patient-a"), "account-doctor-b", Set.of("patient-b"),
                "account-family-a", Set.of("patient-a"), "account-family-b", Set.of("patient-b")));
        var policy = new PatientAccessPolicy(identity -> persistedLinks.getOrDefault(identity.accountId(), Set.of()));
        for (String side : List.of("a", "b")) {
            for (var role : UserRole.values()) {
                String kind = role == UserRole.PHYSICIAN ? "doctor" : "family";
                var identity = new AccountIdentity("account-" + kind + "-" + side, role);
                policy.requireRead(identity, "patient-" + side);
                assertThat(policy.scope(identity)).containsExactly("patient-" + side);
                String other = side.equals("a") ? "b" : "a";
                assertThatThrownBy(() -> policy.requireRead(identity, "patient-" + other))
                        .isInstanceOf(ApplicationFailure.class).hasMessage("PATIENT_ACCESS_DENIED");
            }
        }
        var profileAsAccount = new AccountIdentity("physician-profile-a", UserRole.PHYSICIAN);
        assertThat(policy.scope(profileAsAccount)).isEmpty();
        var doctor = new AccountIdentity("account-doctor-a", UserRole.PHYSICIAN);
        persistedLinks.remove(doctor.accountId());
        assertThatThrownBy(() -> policy.requireRead(doctor, "patient-a")).hasMessage("PATIENT_ACCESS_DENIED");
    }

    @Test
    void indirectResourcePatientMustComeFromStoredRelationship() {
        var policy = new PatientAccessPolicy(identity -> Set.of("patient-a"));
        var doctor = new AccountIdentity("account-doctor", UserRole.PHYSICIAN);
        Map<String, String> resourcePatients = Map.of("alert-b", "patient-b", "record-b", "patient-b", "intervention-b", "patient-b");
        resourcePatients.forEach((id, patientId) ->
                assertThatThrownBy(() -> policy.requireRead(doctor, patientId)).hasMessage("PATIENT_ACCESS_DENIED"));
    }
}
