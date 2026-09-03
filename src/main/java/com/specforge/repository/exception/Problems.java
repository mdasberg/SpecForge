package com.specforge.repository.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * The failures this module reports to a caller. They are thrown as {@link ErrorResponseException}
 * so Spring renders them as RFC 9457 problem documents, the same shape everything else in the API
 * uses.
 */
public final class Problems {

    private Problems() {}

    public static ErrorResponseException notFound(final String detail) {
        return of(HttpStatus.NOT_FOUND, detail);
    }

    public static ErrorResponseException conflict(final String detail) {
        return of(HttpStatus.CONFLICT, detail);
    }

    public static ErrorResponseException unprocessable(final String detail) {
        return of(HttpStatus.UNPROCESSABLE_ENTITY, detail);
    }

    public static ErrorResponseException unauthorized(final String detail) {
        return of(HttpStatus.UNAUTHORIZED, detail);
    }

    private static ErrorResponseException of(final HttpStatus status, final String detail) {
        return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), null);
    }
}
