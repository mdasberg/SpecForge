package com.specforge.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** A valid token lacking the required role is refused as problem+json, not an empty 403. */
@Component
class ProblemAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemResponses problems;

    ProblemAccessDeniedHandler(ProblemResponses problems) {
        this.problems = problems;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        problems.write(request, response, HttpStatus.FORBIDDEN, "This role is not permitted to perform that action.");
    }
}
