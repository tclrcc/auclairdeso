package fr.auclairdeso.billing;

import fr.auclairdeso.booking.AppointmentNotice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

/**
 * The bill of a completed session: the "note" of the arrêté 83-50/A, given to the client before she pays.
 * <p>
 * Its content is a copy taken at issue time: a later change of price, name or address never alters
 * a bill already given. Only its payment changes.
 */
@Entity
@Table(name = "bill")
class Bill {

    /** The practitioner is a micro-entrepreneur under the VAT franchise: no VAT on her bills. */
    static final String VAT_MENTION = "TVA non applicable, article 293 B du CGI";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number", nullable = false, updatable = false, length = 20)
    private String number;

    @Column(name = "appointment_id", nullable = false, updatable = false)
    private Long appointmentId;

    @Column(name = "issued_on", nullable = false, updatable = false)
    private LocalDate issuedOn;

    @Column(name = "practitioner_name", nullable = false, updatable = false, length = 120)
    private String practitionerName;

    @Column(name = "practitioner_address", nullable = false, updatable = false, length = 300)
    private String practitionerAddress;

    @Column(name = "siret", nullable = false, updatable = false, length = 20)
    private String siret;

    @Column(name = "client_name", nullable = false, updatable = false, length = 170)
    private String clientName;

    @Column(name = "client_email", updatable = false, length = 254)
    private @Nullable String clientEmail;

    @Column(name = "service_start", nullable = false, updatable = false)
    private Instant serviceStart;

    @Column(name = "service_place", nullable = false, updatable = false, length = 400)
    private String servicePlace;

    @Column(name = "description", nullable = false, updatable = false, length = 200)
    private String description;

    @Column(name = "amount_cents", nullable = false, updatable = false)
    private int amountCents;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private @Nullable PaymentMethod paymentMethod;

    @Column(name = "paid_on")
    private @Nullable LocalDate paidOn;

    protected Bill() {
        // required by JPA
    }

    /** The bill of a session, at the price agreed when it was booked. */
    static Bill issue(String number, LocalDate issuedOn, AppointmentNotice session, BillingProperties issuer) {
        var bill = new Bill();
        bill.number = number;
        bill.appointmentId = session.id();
        bill.issuedOn = issuedOn;
        bill.practitionerName = issuer.practitionerName().strip();
        bill.practitionerAddress = issuer.address().strip();
        bill.siret = issuer.siret().strip();
        bill.clientName = session.clientFirstName() + " " + session.clientLastName();
        bill.clientEmail = session.clientEmail();
        bill.serviceStart = session.start().atZone(issuer.zone()).toInstant();
        bill.servicePlace = placeOf(session);
        bill.description = session.offeringName();
        bill.amountCents = session.priceCents();
        return bill;
    }

    /**
     * Records the payment. A mistake is corrected by cancelling it first: the receipt book never
     * changes silently.
     */
    void pay(PaymentMethod method, LocalDate paidOn, LocalDate today) {
        if (this.paidOn != null) {
            throw new BillingRefusedException(HttpStatus.CONFLICT, "Cette note est déjà encaissée.");
        }
        if (paidOn.isAfter(today)) {
            throw new BillingRefusedException(HttpStatus.BAD_REQUEST,
                "La date d'encaissement ne peut pas être dans le futur.");
        }
        this.paymentMethod = method;
        this.paidOn = paidOn;
    }

    void cancelPayment() {
        if (paidOn == null) {
            throw new BillingRefusedException(HttpStatus.CONFLICT, "Cette note n'est pas encaissée.");
        }
        this.paymentMethod = null;
        this.paidOn = null;
    }

    BillView toView(ZoneId zone) {
        return new BillView(id, number, appointmentId, issuedOn, practitionerName, practitionerAddress, siret,
            clientName, clientEmail, serviceStart.atZone(zone).toOffsetDateTime(), servicePlace, description,
            amountCents, VAT_MENTION, paymentMethod, paidOn);
    }

    BillNotice toNotice(ZoneId zone, String clientFirstName) {
        return new BillNotice(number, issuedOn, practitionerName, practitionerAddress, siret, clientFirstName,
            clientName, clientEmail, serviceStart.atZone(zone).toLocalDateTime(), servicePlace, description,
            amountCents, VAT_MENTION);
    }

    /** Only for a paid bill: the receipt book lists payments. */
    ReceiptEntry toReceiptEntry() {
        if (paymentMethod == null || paidOn == null) {
            throw new IllegalStateException("Bill " + number + " is not paid");
        }
        return new ReceiptEntry(id, appointmentId, paidOn, number, clientName, description, paymentMethod,
            amountCents);
    }

    /** "The place where the service was performed", one of the mentions of the arrêté. */
    private static String placeOf(AppointmentNotice session) {
        return switch (session.mode()) {
            case IN_PERSON -> "Au cabinet de la praticienne";
            case CLIENT_HOME -> "Au domicile de la cliente : " + session.address();
            case VIDEO -> "À distance, en visio";
            case PHONE -> "À distance, par téléphone";
        };
    }
}
