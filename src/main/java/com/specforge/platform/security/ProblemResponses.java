package com.specforge.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Renders an RFC 9457 problem document from inside the security filter chain, which runs before
 * Spring MVC's exception handling and would otherwise return an empty 401 or 403 body.
 */
@RequiredArgsConstructor
@Component
class ProblemResponses {

    private final JsonMapper json;

    void write(final HttpServletRequest request, final HttpServletResponse response, final HttpStatus status,
            final String detail) throws IOException {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.writeValueAsString(problem));
    }
}
