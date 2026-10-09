package fr.auclairdeso.booking;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/bookings")
class BookingController {

    private final BookingRequests bookings;

    BookingController(BookingRequests bookings) {
        this.bookings = bookings;
    }

    /** A JSON part "request" and, for clairvoyance, an image part "photo". */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    AppointmentView request(@Valid @RequestPart("request") BookingRequest request,
                            @RequestPart(name = "photo", required = false) @Nullable MultipartFile photo,
                            Authentication authentication) throws IOException {
        var portrait = photo == null || photo.isEmpty() ? null : Photo.of(photo.getBytes());
        return bookings.request(authentication.getName(), request, portrait);
    }

    @GetMapping("/mine")
    List<AppointmentView> mine(Authentication authentication) {
        return bookings.mine(authentication.getName());
    }

    /** Details already known about the client, or 204 on her first booking. */
    @GetMapping("/profile")
    ResponseEntity<ClientProfile> profile(Authentication authentication) {
        return bookings.profile(authentication.getName())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/cancel")
    AppointmentView cancel(@PathVariable long id, Authentication authentication) {
        return bookings.cancel(authentication.getName(), id);
    }
}
