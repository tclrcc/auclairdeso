package fr.auclairdeso.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class OfferingTests {

    @Test
    void depositPolicyRequiresADepositNotGreaterThanThePrice() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Offering("guidance", draft(PaymentPolicy.DEPOSIT_ONLINE, null)));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Offering("guidance", draft(PaymentPolicy.DEPOSIT_ONLINE, 9_000)));
    }

    @Test
    void otherPoliciesRejectADeposit() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Offering("guidance", draft(PaymentPolicy.FULL_ONLINE, 1_000)));
    }

    @Test
    void viewListsModesInDeclarationOrder() {
        var view = new Offering("guidance", draft(PaymentPolicy.FULL_ONLINE, null)).toView();

        assertThat(view.modes()).containsExactly(ConsultationMode.IN_PERSON, ConsultationMode.PHONE);
    }

    @Test
    void updateReplacesTheContentButKeepsTheSlug() {
        var offering = new Offering("guidance", draft(PaymentPolicy.FULL_ONLINE, null));

        offering.update(new OfferingDraft("  Guidance renommée ", "Nouvelle description", 45, 6_000,
            PaymentPolicy.DEPOSIT_ONLINE, 2_000, 5, true, Set.of(ConsultationMode.VIDEO)));

        var details = offering.toDetails();
        assertThat(details.slug()).isEqualTo("guidance");
        assertThat(details.name()).isEqualTo("Guidance renommée");
        assertThat(details.depositCents()).isEqualTo(2_000);
        assertThat(details.modes()).containsExactly(ConsultationMode.VIDEO);
    }

    private static OfferingDraft draft(PaymentPolicy policy, @Nullable Integer depositCents) {
        return new OfferingDraft("Guidance", "Description", 60, 8_000, policy, depositCents, 0, true,
            Set.of(ConsultationMode.PHONE, ConsultationMode.IN_PERSON));
    }
}
