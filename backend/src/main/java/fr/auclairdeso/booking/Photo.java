package fr.auclairdeso.booking;

import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 * A portrait sent by a client, whose format is recognised from its first bytes
 * rather than trusted from what the browser declares.
 */
record Photo(String contentType, byte[] data) {

    static final int MAX_BYTES = 10 * 1024 * 1024;

    static Photo of(byte[] data) {
        if (data.length == 0 || data.length > MAX_BYTES) {
            throw new BookingRefusedException(HttpStatus.BAD_REQUEST, "La photo doit faire au plus 10 Mo.");
        }
        var contentType = detectType(data).orElseThrow(() -> new BookingRefusedException(HttpStatus.BAD_REQUEST,
            "La photo doit être au format JPEG, PNG ou WebP."));
        return new Photo(contentType, data);
    }

    private static Optional<String> detectType(byte[] data) {
        if (startsWith(data, 0, 0xFF, 0xD8, 0xFF)) {
            return Optional.of("image/jpeg");
        }
        if (startsWith(data, 0, 0x89, 'P', 'N', 'G')) {
            return Optional.of("image/png");
        }
        if (startsWith(data, 0, 'R', 'I', 'F', 'F') && startsWith(data, 8, 'W', 'E', 'B', 'P')) {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, int offset, int... expected) {
        if (data.length < offset + expected.length) {
            return false;
        }
        for (var i = 0; i < expected.length; i++) {
            if ((data[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
