package fr.auclairdeso.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Another offering already uses the slug derived from this name; rendered as a 409 ProblemDetail.
 */
final class OfferingNameTakenException extends ErrorResponseException {

    OfferingNameTakenException(String slug) {
        super(HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "Une séance porte déjà un nom très proche (« %s »). Choisissez un autre nom.".formatted(slug)), null);
    }
}
