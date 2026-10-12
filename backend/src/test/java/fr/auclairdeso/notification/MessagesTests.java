package fr.auclairdeso.notification;

import static org.assertj.core.api.Assertions.assertThat;

import fr.auclairdeso.billing.BillNotice;
import fr.auclairdeso.booking.AppointmentNotice;
import fr.auclairdeso.booking.EveningProgram;
import fr.auclairdeso.catalog.ConsultationMode;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessagesTests {

    private final Messages messages = new Messages(URI.create("https://auclairdeso.example/"),
        new NotificationProperties("06 00 00 00 00", "1 place de la Mairie, 01150 Chazey-sur-Ain"));

    @Test
    void theConfirmationSaysWhenHowAndUntilWhenToCancelFreely() {
        var message = messages.confirmed(notice(ConsultationMode.PHONE));

        assertThat(message.subject()).isEqualTo("Rendez-vous confirmé : lundi 19 octobre à 14h00");
        assertThat(message.text())
            .contains("Bonjour Alice,")
            .contains("80 €, à régler après la séance")
            .contains("au 06 00 00 00 00")
            .contains("après le samedi 17 octobre à 14h00")
            .contains("https://auclairdeso.example/mon-espace");
    }

    @Test
    void theAddressOfThePracticeIsGivenOnlyForAnInPersonSession() {
        assertThat(messages.confirmed(notice(ConsultationMode.IN_PERSON)).text()).contains("1 place de la Mairie");
        assertThat(messages.confirmed(notice(ConsultationMode.VIDEO)).text())
            .doesNotContain("1 place de la Mairie")
            .contains("sur le compte « Alice M »");
    }

    @Test
    void thePractitionerGetsALinkToAnswerTheRequest() {
        var message = messages.newRequest(notice(ConsultationMode.VIDEO));

        assertThat(message.subject()).isEqualTo("Nouvelle demande : Alice Martin, lundi 19 octobre à 14h00");
        assertThat(message.text()).contains("https://auclairdeso.example/admin/agenda/7");
    }

    @Test
    void theEveningProgramListsTomorrowsSessionsThenThePendingRequests() {
        var program = new EveningProgram(LocalDate.of(2026, 10, 19),
            List.of(notice(ConsultationMode.VIDEO)), List.of(notice(ConsultationMode.PHONE)));

        var text = messages.eveningProgram(program).text();

        assertThat(text).contains("- 14h00 : Alice Martin, Consultation approfondie, en visio");
        assertThat(text).contains("Demandes en attente de réponse : 1");
        assertThat(text.indexOf("Demain")).isLessThan(text.indexOf("Demandes en attente"));
    }

    @Test
    void aLateCancellationIsSaid() {
        assertThat(messages.cancelledByClient(notice(ConsultationMode.VIDEO), true, true).text())
            .contains("comptée comme tardive");
        assertThat(messages.cancelledByClient(notice(ConsultationMode.VIDEO), false, false).text())
            .contains("Votre demande pour le lundi 19 octobre à 14h00")
            .doesNotContain("tardive");
    }

    @Test
    void theBillCarriesEveryMentionTheLawAsksFor() {
        var bill = new BillNotice("2026-0007", LocalDate.of(2026, 10, 19), "Sophie Exemple EI",
            "1 place de la Mairie, 01150 Chazey-sur-Ain", "123 456 789 00012", "Alice", "Alice Martin",
            "alice@example.com", LocalDateTime.of(2026, 10, 19, 14, 0), "À distance, en visio",
            "Consultation approfondie", 8_050, "TVA non applicable, article 293 B du CGI");

        var message = messages.bill(bill);

        assertThat(message.subject()).isEqualTo("Votre note n° 2026-0007 : séance du 19 octobre 2026");
        assertThat(message.text())
            .contains("Établie le 19 octobre 2026")
            .contains("Sophie Exemple EI\n1 place de la Mairie, 01150 Chazey-sur-Ain\nSIRET : 123 456 789 00012")
            .contains("Cliente : Alice Martin")
            .contains("Séance du lundi 19 octobre 2026 à 14h00")
            .contains("Lieu : À distance, en visio")
            .contains("1 × Consultation approfondie : 80,50 €")
            .contains("Total à payer : 80,50 €")
            .contains("TVA non applicable, article 293 B du CGI");
    }

    private static AppointmentNotice notice(ConsultationMode mode) {
        return new AppointmentNotice(7, "alice@example.com", "Alice", "Martin", "06 12 34 56 78",
            "Consultation approfondie", mode, LocalDateTime.of(2026, 10, 19, 14, 0),
            LocalDateTime.of(2026, 10, 17, 14, 0), 8_000, null,
            mode == ConsultationMode.VIDEO ? "Alice M" : null);
    }
}
