package com.specforge;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Shared base for the `itest` source set, which is the integration suite: application.yaml
 * already points at the docker-compose PostgreSQL, never H2, and every subclass shares one cached
 * Spring context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    /**
     * The JDK client rather than a Spring one: these tests assert on 401 and 403 responses, and a
     * client that throws on a 4xx would turn the thing under test into an exception.
     */
    /**
     * Every request is bounded. Without this a service that accepts a connection and then never
     * answers hangs the suite forever rather than failing it, which is the harder failure to
     * diagnose — CI shows no output until something eventually times out.
     */
    static final Duration TIMEOUT = Duration.ofSeconds(20);

    protected static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    @LocalServerPort
    private int port;

    protected HttpResponse<String> get(String path, String bearerToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (bearerToken != null) {
            request.header("Authorization", "Bearer " + bearerToken);
        }
        return HTTP.send(request.timeout(TIMEOUT).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
