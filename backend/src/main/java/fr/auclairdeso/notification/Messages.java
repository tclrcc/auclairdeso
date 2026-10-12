package fr.auclairdeso.notification;

import fr.auclairdeso.billing.BillNotice;
import fr.auclairdeso.booking.AppointmentNotice;
import fr.auclairdeso.booking.EveningProgram;
import fr.auclairdeso.catalog.ConsultationMode;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * The words of every email (docs/specification.md, 7). Plain text: read the same everywhere,
 * and the reason of a request never leaves the site (it can hold health information).
 */
final class Messages {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H'h'mm", Locale.FRANCE);
    private static final DateTimeFormatter FULL_DAY = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE);
    private static final String SIGNATURE = "Au clair de So";

    private final String site;
    private final @Nullable String practitionerPhone;
    private final @Nullable String practiceAddress;

    Messages(URI site, NotificationProperties properties) {
        this.site = site.toString().replaceAll("/+$", "");
        this.practitionerPhone = blankToNull(properties.practitionerPhone());
        this.practiceAddress = blankToNull(properties.practiceAddress());
    }

    // ---- To the practitioner

    Message newRequest(AppointmentNotice appointment) {
        return new Message(
            "Nouvelle demande : %s, %s".formatted(name(appointment), when(appointment.start())),
            """
            Nouvelle demande de réservation :

            %s, %s
            %s
            %s, %s

            Pour la lire et y répondre :
            %s

            Sans réponse, la demande expire la veille de la séance au soir, et le créneau est libéré.
            """.formatted(appointment.offeringName(), mode(appointment.mode()),
                capitalize(when(appointment.start())), name(appointment), appointment.clientPhone(),
                agendaLink(appointment)));
    }

    Message clientCancelled(AppointmentNotice appointment, boolean wasConfirmed, boolean late) {
        var what = wasConfirmed ? "a annulé son rendez-vous" : "a retiré sa demande";
        return new Message(
            "%s : %s, %s".formatted(wasConfirmed ? "Annulation" : "Demande retirée", name(appointment),
                when(appointment.start())),
            """
            %s %s du %s (%s, %s).%s
            Le créneau est de nouveau libre.

            %s
            """.formatted(name(appointment), what, when(appointment.start()), appointment.offeringName(),
                mode(appointment.mode()), late ? "\nC'est une annulation tardive." : "",
                agendaLink(appointment)));
    }

    Message eveningProgram(EveningProgram program) {
        var text = new StringBuilder();
        var day = DAY.format(program.day());
        if (program.sessions().isEmpty()) {
            text.append("Aucune séance confirmée demain, ").append(day).append(".\n");
        } else {
            text.append("Demain, ").append(day).append(" :\n\n");
            for (var session : program.sessions()) {
                text.append("- ").append(TIME.format(session.start())).append(" : ").append(name(session))
                    .append(", ").append(session.offeringName()).append(", ").append(mode(session.mode()))
                    .append(" (").append(session.clientPhone()).append(")\n");
            }
        }
        if (!program.pendingRequests().isEmpty()) {
            text.append("\nDemandes en attente de réponse : ").append(program.pendingRequests().size())
                .append("\n\n");
            for (var request : program.pendingRequests()) {
                text.append("- ").append(when(request.start())).append(" : ").append(name(request))
                    .append(", ").append(request.offeringName()).append("\n");
            }
        }
        text.append("\nVotre agenda :\n").append(site).append("/admin/agenda\n");
        return new Message("Votre programme pour demain, " + day, text.toString());
    }

    // ---- To the client

    Message confirmed(AppointmentNotice appointment) {
        return new Message(
            "Rendez-vous confirmé : " + when(appointment.start()),
            """
            Bonjour %s,

            Votre rendez-vous est confirmé :

            %s, %s
            %s
            %s, à régler après la séance, par virement, en espèces ou par paiement mobile.

            %s

            En cas d'empêchement, annulez depuis vos rendez-vous :
            %s/mon-espace
            Une annulation après le %s est comptée comme tardive.

            À bientôt,
            %s
            """.formatted(appointment.clientFirstName(), appointment.offeringName(), mode(appointment.mode()),
                capitalize(when(appointment.start())), euros(appointment.priceCents()),
                practicalDetails(appointment), site, when(appointment.freeCancellationUntil()),
                signature()));
    }

    Message reminder(AppointmentNotice appointment) {
        return new Message(
            "Rappel : votre séance demain à " + TIME.format(appointment.start()),
            """
            Bonjour %s,

            Petit rappel : votre séance a lieu demain, %s.

            %s, %s
            %s

            En cas d'empêchement, merci d'annuler au plus vite depuis vos rendez-vous :
            %s/mon-espace

            À demain,
            %s
            """.formatted(appointment.clientFirstName(), when(appointment.start()), appointment.offeringName(),
                mode(appointment.mode()), practicalDetails(appointment), site, signature()));
    }

    Message declined(AppointmentNotice appointment) {
        return new Message(
            "Votre demande du " + when(appointment.start()),
            """
            Bonjour %s,

            So n'est pas disponible le %s pour la séance « %s » : votre demande n'a pas pu être acceptée.

            Vous pouvez choisir un autre créneau :
            %s/seances

            %s
            """.formatted(appointment.clientFirstName(), when(appointment.start()), appointment.offeringName(),
                site, signature()));
    }

    Message expired(AppointmentNotice appointment) {
        return new Message(
            "Votre demande du " + when(appointment.start()),
            """
            Bonjour %s,

            Votre demande pour le %s (« %s ») n'a pas pu être confirmée à temps : le créneau est libéré.

            Vous pouvez choisir un autre créneau :
            %s/seances

            %s
            """.formatted(appointment.clientFirstName(), when(appointment.start()), appointment.offeringName(),
                site, signature()));
    }

    Message cancelledByPractitioner(AppointmentNotice appointment) {
        return new Message(
            "Rendez-vous annulé : " + when(appointment.start()),
            """
            Bonjour %s,

            So a dû annuler votre rendez-vous du %s (« %s »).

            Vous pouvez choisir un nouveau créneau :
            %s/seances

            %s
            """.formatted(appointment.clientFirstName(), when(appointment.start()), appointment.offeringName(),
                site, signature()));
    }

    Message cancelledByClient(AppointmentNotice appointment, boolean wasConfirmed, boolean late) {
        var what = wasConfirmed ? "Votre rendez-vous du %s (« %s ») est bien annulé."
            : "Votre demande pour le %s (« %s ») est bien annulée.";
        var lateness = late
            ? "\nCette annulation intervient après le %s : elle est comptée comme tardive.\n"
            .formatted(when(appointment.freeCancellationUntil()))
            : "";
        return new Message(
            "Annulation enregistrée : " + when(appointment.start()),
            """
            Bonjour %s,

            %s
            %s
            Vous pouvez réserver un autre créneau à tout moment :
            %s/seances

            %s
            """.formatted(appointment.clientFirstName(),
                what.formatted(when(appointment.start()), appointment.offeringName()), lateness, site,
                signature()));
    }

    /**
     * The bill itself, with every mention the arrêté 83-50/A requires: date, provider with name and address,
     * client, date and place of the session, detail and total. Plain text, so that it prints as it is.
     */
    Message bill(BillNotice bill) {
        var amount = exactEuros(bill.amountCents());
        return new Message(
            "Votre note n° %s : séance du %s".formatted(bill.number(), DATE.format(bill.serviceStart())),
            """
            Bonjour %s,

            Merci pour votre séance. Voici votre note, à garder.

            NOTE N° %s
            Établie le %s

            %s
            %s
            SIRET : %s

            Cliente : %s
            Séance du %s
            Lieu : %s

            1 × %s : %s
            Total hors taxes : %s
            Total à payer : %s
            %s

            Règlement par virement, en espèces ou par paiement mobile.

            À bientôt,
            %s
            """.formatted(bill.clientFirstName(), bill.number(), DATE.format(bill.issuedOn()),
                bill.practitionerName(), bill.practitionerAddress(), bill.siret(), bill.clientName(),
                when(bill.serviceStart(), FULL_DAY), bill.servicePlace(), bill.description(),
                amount, amount, amount, bill.vatMention(), signature()));
    }

    // ---- Shared pieces

    /** Where the session takes place, or who calls whom: it depends on the mode. */
    private String practicalDetails(AppointmentNotice appointment) {
        return switch (appointment.mode()) {
            case IN_PERSON -> practiceAddress != null
                ? "La séance a lieu à l'adresse suivante :\n" + practiceAddress
                : "So vous indiquera l'adresse de la séance.";
            case CLIENT_HOME -> "So viendra chez vous, à l'adresse indiquée :\n" + appointment.address();
            case VIDEO -> "So vous appellera en visio sur Messenger à l'heure du rendez-vous, sur le compte « %s »."
                .formatted(appointment.messengerName());
            case PHONE -> practitionerPhone != null
                ? "C'est vous qui appelez So à l'heure du rendez-vous, au " + practitionerPhone + "."
                : "C'est vous qui appelez So à l'heure du rendez-vous : elle vous indiquera son numéro.";
        };
    }

    private String signature() {
        return SIGNATURE + "\n" + site;
    }

    private String agendaLink(AppointmentNotice appointment) {
        return site + "/admin/agenda/" + appointment.id();
    }

    private static String mode(ConsultationMode mode) {
        return switch (mode) {
            case IN_PERSON -> "en présentiel";
            case CLIENT_HOME -> "à domicile";
            case VIDEO -> "en visio";
            case PHONE -> "par téléphone";
        };
    }

    /** "lundi 19 octobre à 14h00" */
    private static String when(LocalDateTime time) {
        return when(time, DAY);
    }

    private static String when(LocalDateTime time, DateTimeFormatter day) {
        return day.format(time) + " à " + TIME.format(time);
    }

    private static String name(AppointmentNotice appointment) {
        return appointment.clientFirstName() + " " + appointment.clientLastName();
    }

    /** "80 €", or "12,50 €" when there are cents. */
    private static String euros(int cents) {
        return cents % 100 == 0
            ? (cents / 100) + " €"
            : String.format(Locale.FRANCE, "%.2f €", cents / 100.0);
    }

    /** "80,00 €": a bill always shows the cents. */
    private static String exactEuros(int cents) {
        return String.format(Locale.FRANCE, "%d,%02d €", cents / 100, cents % 100);
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
