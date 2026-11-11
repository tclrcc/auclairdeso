package fr.auclairdeso.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The video of an offering is a file dropped by hand in MEDIA_DIR and served under /contenus/: its name is
 * the only thing the application stores, so it must stay a plain file name (docs/specification.md, 3.6).
 */
class OfferingDraftTests {

    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"guidance-1-h.mp4", "guidance-30-min.webm", "a.mp4", "1-seance.mp4"})
    void acceptsAPlainVideoFileName(String path) {
        assertThat(violatedFields(draft(path, null))).isEmpty();
        assertThat(draft(path, null).videoPath()).isEqualTo(path);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "videos/guidance.mp4", "/guidance.mp4", "../guidance.mp4", "..", "guidance..mp4", "a/../b.mp4",
        "videos\\guidance.mp4", "Guidance.mp4", "guidance.MP4", "guidance.mov", "guidance", ".mp4", "-guidance.mp4",
        "guidance_1h.mp4", "guidance 1h.mp4", "guidance.mp4.exe", "guidance.mp4/",
        "https://example.com/guidance.mp4",
    })
    void rejectsAnythingButAPlainVideoFileName(String path) {
        assertThat(violatedFields(draft(path, null))).containsExactly("videoPath");
    }

    @Test
    void rejectsAVideoFileNameLongerThanTheColumn() {
        var tooLong = "a".repeat(97) + ".mp4";

        assertThat(violatedFields(draft(tooLong, null))).contains("videoPath");
        assertThat(violatedFields(draft("a".repeat(96) + ".mp4", null))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n"})
    void anEmptyVideoPathOrTranscriptMeansNoVideo(String empty) {
        var draft = draft(empty, empty);

        assertThat(violatedFields(draft)).isEmpty();
        assertThat(draft.videoPath()).isNull();
        assertThat(draft.videoTranscript()).isNull();
    }

    @Test
    void acceptsATranscriptUpToFourThousandCharacters() {
        assertThat(violatedFields(draft(null, "a".repeat(4_000)))).isEmpty();
        assertThat(violatedFields(draft(null, "a".repeat(4_001)))).containsExactly("videoTranscript");
    }

    @Test
    void aVideoPathIsStoredWithoutItsSurroundingBlanks() {
        assertThat(violatedFields(draft(" guidance.mp4\n", null))).isEmpty();
        assertThat(draft(" guidance.mp4\n", null).videoPath()).isEqualTo("guidance.mp4");
    }

    @Test
    void aTranscriptIsStoredWithoutItsSurroundingBlanks() {
        assertThat(draft(null, "  Bonjour, je suis So.\n").videoTranscript()).isEqualTo("Bonjour, je suis So.");
    }

    private static Set<String> violatedFields(OfferingDraft draft) {
        return validator.validate(draft).stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }

    private static OfferingDraft draft(String videoPath, String videoTranscript) {
        return new OfferingDraft("Guidance", "Description", 60, 15, 8_000, PaymentPolicy.ON_SITE, null, 0, true,
            Set.of(ConsultationMode.PHONE), OfferingCategory.CLAIRVOYANCE, videoPath, videoTranscript);
    }
}
