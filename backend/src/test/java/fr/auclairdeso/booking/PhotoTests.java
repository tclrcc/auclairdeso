package fr.auclairdeso.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PhotoTests {

    @Test
    void recognisesJpegPngAndWebp() {
        assertThat(Photo.of(bytes(0xFF, 0xD8, 0xFF, 0xE0)).contentType()).isEqualTo("image/jpeg");
        assertThat(Photo.of(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A)).contentType()).isEqualTo("image/png");
        assertThat(Photo.of(bytes('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P')).contentType())
            .isEqualTo("image/webp");
    }

    @Test
    void refusesAFileDisguisedAsAnImage() {
        var svg = "<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8);

        assertThatExceptionOfType(BookingRefusedException.class).isThrownBy(() -> Photo.of(svg));
    }

    @Test
    void refusesAnEmptyFile() {
        assertThatExceptionOfType(BookingRefusedException.class).isThrownBy(() -> Photo.of(new byte[0]));
    }

    private static byte[] bytes(int... values) {
        var data = new byte[values.length];
        for (var i = 0; i < values.length; i++) {
            data[i] = (byte) values[i];
        }
        return data;
    }
}
