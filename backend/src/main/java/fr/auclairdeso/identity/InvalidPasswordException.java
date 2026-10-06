package fr.auclairdeso.identity;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * A new password that does not meet the policy; rendered as a 400 ProblemDetail.
 */
final class InvalidPasswordException extends ErrorResponseException {

    InvalidPasswordException(String detail) {
        super(HttpStatus.BAD_REQUEST, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail), null);
    }
}
