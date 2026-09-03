package com.specforge.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** A request to {@code /api/**} without a valid token is refused as problem+json, not an empty 401. */
@RequiredArgsConstructor
@Component
class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemResponses problems;

    @Override
    public void commence(final HttpServletRequest request, final HttpServletResponse response,
            final AuthenticationException exception)
            throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        problems.write(request, response, HttpStatus.UNAUTHORIZED, "Authentication is required.");
    }
}
