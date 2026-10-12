package fr.auclairdeso.billing;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** A billing rule that is not met; rendered as a ProblemDetail with a message for the practitioner. */
final class BillingRefusedException extends ErrorResponseException {

    BillingRefusedException(HttpStatus status, String detail) {
        super(status, ProblemDetail.forStatusAndDetail(status, detail), null);
    }
}
