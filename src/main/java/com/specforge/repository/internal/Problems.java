package com.specforge.repository.internal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * The failures this module reports to a caller. They are thrown as {@link ErrorResponseException}
 * so Spring renders them as RFC 9457 problem documents, the same shape everything else in the API
 * uses.
 */
final class Problems {

    private Problems() {}

    static ErrorResponseException notFound(String detail) {
        return of(HttpStatus.NOT_FOUND, detail);
    }

    static ErrorResponseException conflict(String detail) {
        return of(HttpStatus.CONFLICT, detail);
    }

    static ErrorResponseException unprocessable(String detail) {
        return of(HttpStatus.UNPROCESSABLE_ENTITY, detail);
    }

    static ErrorResponseException unauthorized(String detail) {
        return of(HttpStatus.UNAUTHORIZED, detail);
    }

    private static ErrorResponseException of(HttpStatus status, String detail) {
        return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), null);
    }
}
