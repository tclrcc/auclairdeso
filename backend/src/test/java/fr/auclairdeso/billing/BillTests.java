package fr.auclairdeso.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import fr.auclairdeso.booking.AppointmentNotice;
import fr.auclairdeso.catalog.ConsultationMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class BillTests {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);
    private static final BillingProperties ISSUER = new BillingProperties(PARIS, "Sophie Exemple EI",
        "1 place de la Mairie, 01150 Chazey-sur-Ain", "123 456 789 00012");

    @Test
    void theBillCopiesEveryMentionAtIssueTime() {
        var bill = Bill.issue("2026-0007", TODAY, session(ConsultationMode.CLIENT_HOME), ISSUER).toView(PARIS);

        assertThat(bill.number()).isEqualTo("2026-0007");
        assertThat(bill.clientName()).isEqualTo("Alice Martin");
        assertThat(bill.servicePlace()).isEqualTo("Au domicile de la cliente : 3 rue des Lilas, Lagnieu");
        assertThat(bill.serviceStart().toString()).isEqualTo("2026-10-19T14:00+02:00");
        assertThat(bill.amountCents()).isEqualTo(8_000);
        assertThat(bill.vatMention()).isEqualTo("TVA non applicable, article 293 B du CGI");
        assertThat(bill.paidOn()).isNull();
    }

    @Test
    void aBillIsPaidOnceAndNeverInTheFuture() {
        var bill = Bill.issue("2026-0007", TODAY, session(ConsultationMode.VIDEO), ISSUER);

        assertThatExceptionOfType(BillingRefusedException.class)
            .isThrownBy(() -> bill.pay(PaymentMethod.CASH, TODAY.plusDays(1), TODAY));
        bill.pay(PaymentMethod.TRANSFER, TODAY, TODAY);
        assertThatExceptionOfType(BillingRefusedException.class)
            .isThrownBy(() -> bill.pay(PaymentMethod.CASH, TODAY, TODAY));

        assertThat(bill.toReceiptEntry().method()).isEqualTo(PaymentMethod.TRANSFER);
    }

    @Test
    void aMistakenPaymentIsCancelledThenRecordedAgain() {
        var bill = Bill.issue("2026-0007", TODAY, session(ConsultationMode.PHONE), ISSUER);
        assertThatExceptionOfType(BillingRefusedException.class).isThrownBy(bill::cancelPayment);

        bill.pay(PaymentMethod.CASH, TODAY, TODAY);
        bill.cancelPayment();
        bill.pay(PaymentMethod.MOBILE, TODAY, TODAY);

        assertThat(bill.toView(PARIS).paymentMethod()).isEqualTo(PaymentMethod.MOBILE);
    }

    private static AppointmentNotice session(ConsultationMode mode) {
        return new AppointmentNotice(7, "alice@example.com", "Alice", "Martin", "06 12 34 56 78",
            "Consultation approfondie", mode, LocalDateTime.of(2026, 10, 19, 14, 0),
            LocalDateTime.of(2026, 10, 17, 14, 0), 8_000,
            mode == ConsultationMode.CLIENT_HOME ? "3 rue des Lilas, Lagnieu" : null, null);
    }
}
