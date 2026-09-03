package com.specforge;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fetches real tokens from the composed Keycloak. The tests deliberately do not mint their own
 * JWTs: the point is that the resource server accepts what this realm actually issues, signed by
 * its actual keys.
 */
final class Keycloak {

    static final String ISSUER = "http://localhost:8081/realms/specforge";
    private static final String ADMIN = "http://localhost:8081/admin/realms/specforge";

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private Keycloak() {
    }

    /** A human's token, through the development realm's direct access grant. */
    static String passwordToken(String username, String password) {
        return token(Map.of(
                "grant_type", "password",
                "client_id", "specforge-web",
                "username", username,
                "password", password));
    }

    /** A check runner's token, through the confidential client's service account. */
    static String serviceAccountToken() {
        return token(Map.of(
                "grant_type", "client_credentials",
                "client_id", "specforge-api",
                "client_secret", "specforge-api-dev-secret"));
    }

    /** An admin token from the master realm, for the handful of tests that must change the realm. */
    static String adminToken() {
        return post(URI.create("http://localhost:8081/realms/master/protocol/openid-connect/token"),
                "application/x-www-form-urlencoded",
                urlEncoded(Map.of("grant_type", "password", "client_id", "admin-cli", "username", "admin", "password", "admin")),
                null,
                200);
    }

    /** The realm role representation the admin API needs as the body of a role-mapping change. */
    static String realmRole(String adminToken, String role) {
        return send("GET", URI.create(ADMIN + "/roles/" + role), null, null, adminToken, 200);
    }

    static String userId(String adminToken, String username) {
        String found = send("GET", URI.create(ADMIN + "/users?username=" + username + "&exact=true"), null, null, adminToken, 200);
        return JSON.readTree(found).get(0).get("id").asString();
    }

    static void addRealmRole(String adminToken, String userId, String roleRepresentation) {
        send("POST", URI.create(ADMIN + "/users/" + userId + "/role-mappings/realm"),
                "application/json", "[" + roleRepresentation + "]", adminToken, 204);
    }

    static void removeRealmRole(String adminToken, String userId, String roleRepresentation) {
        send("DELETE", URI.create(ADMIN + "/users/" + userId + "/role-mappings/realm"),
                "application/json", "[" + roleRepresentation + "]", adminToken, 204);
    }

    private static String send(String method, URI uri, String contentType, String body, String bearerToken, int expected) {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri);
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        if (bearerToken != null) {
            request.header("Authorization", "Bearer " + bearerToken);
        }
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        try {
            HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != expected) {
                throw new IllegalStateException(method + " " + uri + " returned " + response.statusCode() + " " + response.body());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String post(URI uri, String contentType, String body, String bearerToken, int expected) {
        return JSON.readTree(send("POST", uri, contentType, body, bearerToken, expected)).get("access_token").asString();
    }

    private static String token(Map<String, String> form) {
        try {
            HttpResponse<String> response = HTTP.send(
                    HttpRequest.newBuilder(URI.create(ISSUER + "/protocol/openid-connect/token"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(urlEncoded(form)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Keycloak refused the token request: " + response.statusCode() + " " + response.body());
            }
            return JSON.readTree(response.body()).get("access_token").asString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not obtain a token from " + ISSUER, e);
        }
    }

    private static String urlEncoded(Map<String, String> form) {
        Map<String, String> ordered = new LinkedHashMap<>(form);
        StringBuilder body = new StringBuilder();
        ordered.forEach((key, value) -> {
            if (!body.isEmpty()) {
                body.append('&');
            }
            body.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return body.toString();
    }
}
