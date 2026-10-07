package fr.auclairdeso.booking;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** A booking rule that is not met; rendered as a ProblemDetail with a message for the client. */
final class BookingRefusedException extends ErrorResponseException {

    BookingRefusedException(HttpStatus status, String detail) {
        super(status, ProblemDetail.forStatusAndDetail(status, detail), null);
    }
}
