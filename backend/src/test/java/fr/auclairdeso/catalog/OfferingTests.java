package fr.auclairdeso.catalog;

import org.junit.jupiter.api.Test;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class OfferingTests {

    @Test
    void depositPolicyRequiresADepositNotGreaterThanThePrice() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> offering(PaymentPolicy.DEPOSIT_ONLINE, null));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> offering(PaymentPolicy.DEPOSIT_ONLINE, 9_000));
    }

    @Test
    void otherPoliciesRejectADeposit() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> offering(PaymentPolicy.FULL_ONLINE, 1_000));
    }

    @Test
    void viewListsModesInDeclarationOrder() {
        var view = new Offering("guidance-1-h", "Guidance", "Description", 60, 8_000,
            PaymentPolicy.FULL_ONLINE, null, 0,
            Set.of(ConsultationMode.PHONE, ConsultationMode.IN_PERSON)).toView();

        assertThat(view.modes()).containsExactly(ConsultationMode.IN_PERSON, ConsultationMode.PHONE);
    }

    private static Offering offering(PaymentPolicy policy, Integer depositCents) {
        return new Offering("guidance-1-h", "Guidance", "Description", 60, 8_000,
            policy, depositCents, 0, Set.of(ConsultationMode.PHONE));
    }
}
