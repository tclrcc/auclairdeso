package fr.auclairdeso.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class OfferingTests {

    @Test
    void anOfferingIsAlwaysPaidOnSiteWithoutDeposit() {
        for (var policy : PaymentPolicy.values()) {
            var details = new Offering("guidance", draft(policy, 3_000)).toDetails();

            assertThat(details.paymentPolicy()).as("policy received: %s", policy).isEqualTo(PaymentPolicy.ON_SITE);
            assertThat(details.depositCents()).as("deposit received with %s", policy).isNull();
        }
    }

    @Test
    void updatingAnOfferingKeepsItPaidOnSite() {
        var offering = new Offering("guidance", draft(PaymentPolicy.ON_SITE, null));

        offering.update(draft(PaymentPolicy.DEPOSIT_ONLINE, 2_000));

        var details = offering.toDetails();
        assertThat(details.paymentPolicy()).isEqualTo(PaymentPolicy.ON_SITE);
        assertThat(details.depositCents()).isNull();
    }

    @Test
    void viewListsModesInDeclarationOrder() {
        var view = new Offering("guidance", draft(PaymentPolicy.FULL_ONLINE, null)).toView();

        assertThat(view.modes()).containsExactly(ConsultationMode.IN_PERSON, ConsultationMode.PHONE);
    }

    @Test
    void updateReplacesTheContentButKeepsTheSlug() {
        var offering = new Offering("guidance", draft(PaymentPolicy.FULL_ONLINE, null));

        offering.update(new OfferingDraft("  Guidance renommée ", "Nouvelle description", 45, 15, 6_000,
            PaymentPolicy.ON_SITE, null, 5, true, Set.of(ConsultationMode.VIDEO), OfferingCategory.CLAIRVOYANCE));

        var details = offering.toDetails();
        assertThat(details.slug()).isEqualTo("guidance");
        assertThat(details.name()).isEqualTo("Guidance renommée");
        assertThat(details.priceCents()).isEqualTo(6_000);
        assertThat(details.modes()).containsExactly(ConsultationMode.VIDEO);
    }

    private static OfferingDraft draft(PaymentPolicy policy, @Nullable Integer depositCents) {
        return new OfferingDraft("Guidance", "Description", 60, 15, 8_000, policy, depositCents, 0, true,
            Set.of(ConsultationMode.PHONE, ConsultationMode.IN_PERSON), OfferingCategory.CLAIRVOYANCE);
    }
}
