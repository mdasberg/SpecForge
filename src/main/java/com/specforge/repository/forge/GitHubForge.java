package com.specforge.repository.forge;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.specforge.repository.configuration.GitHubProperties;
import com.specforge.repository.exception.ForgeException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


/**
 * The GitHub implementation of the forge port. Reads through short-lived installation tokens: the
 * App's private key signs a JWT, the JWT buys a token scoped to one installation, and that token
 * expires within the hour whether or not SpecForge remembers to stop using it.
 */
@Component
public class GitHubForge implements Forge {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final Duration TOKEN_EARLY_EXPIRY = Duration.ofMinutes(1);

    private final GitHubProperties properties;
    private final ObjectMapper json;
    private final Clock clock;
    private final HttpClient http;
    private final Map<String, InstallationToken> tokens = new ConcurrentHashMap<>();

    GitHubForge(GitHubProperties properties, ObjectMapper json, Clock clock) {
        this.properties = properties;
        this.json = json;
        this.clock = clock;
        this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    @Override
    public Optional<ForgeInstallationInfo> installation(String installationExternalId) {
        Optional<JsonNode> installation = getAsApp("/app/installations/" + installationExternalId);
        if (installation.isEmpty()) {
            return Optional.empty();
        }
        JsonNode node = installation.get();
        JsonNode granted = getAsInstallation(installationExternalId, "/installation/repositories?per_page=100")
                .orElse(json.createObjectNode());
        List<ForgeRepositoryInfo> repositories = new ArrayList<>();
        for (JsonNode repository : granted.path("repositories")) {
            repositories.add(new ForgeRepositoryInfo(
                    repository.path("full_name").asText(),
                    repository.path("default_branch").asText(null),
                    repository.path("id").asText(null)));
        }
        return Optional.of(new ForgeInstallationInfo(
                installationExternalId,
                node.path("account").path("login").asText(null),
                node.path("account").path("type").asText(null),
                node.path("repositories_total_count").isMissingNode()
                        ? null
                        : node.path("repositories_total_count").asInt(),
                !node.path("suspended_at").isNull() && node.hasNonNull("suspended_at"),
                List.copyOf(repositories)));
    }

    @Override
    public List<String> listFiles(String installationExternalId, ForgeRef ref) {
        String path = "/repos/%s/git/trees/%s?recursive=1".formatted(ref.repositoryFullName(), encode(ref.ref()));
        JsonNode tree = getAsInstallation(installationExternalId, path).orElse(json.createObjectNode());
        List<String> files = new ArrayList<>();
        for (JsonNode entry : tree.path("tree")) {
            if ("blob".equals(entry.path("type").asText())) {
                files.add(entry.path("path").asText());
            }
        }
        return List.copyOf(files);
    }

    @Override
    public Optional<ForgeFile> readFile(String installationExternalId, ForgeRef ref, String path) {
        String contentsPath = "/repos/%s/contents/%s?ref=%s"
                .formatted(ref.repositoryFullName(), encodePath(path), encode(ref.ref()));
        Optional<JsonNode> contents = getAsInstallation(installationExternalId, contentsPath);
        if (contents.isEmpty() || !"base64".equals(contents.get().path("encoding").asText())) {
            return Optional.empty();
        }
        // GitHub wraps the base64 payload at 60 columns, which the strict decoder rejects.
        String encoded = contents.get().path("content").asText().replaceAll("\\s", "");
        String content = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);

        String commitsPath = "/repos/%s/commits?path=%s&sha=%s&per_page=1"
                .formatted(ref.repositoryFullName(), encodePath(path), encode(ref.ref()));
        JsonNode commits = getAsInstallation(installationExternalId, commitsPath).orElse(json.createArrayNode());
        JsonNode last = commits.path(0);
        return Optional.of(new ForgeFile(
                path,
                content,
                last.path("sha").asText(null),
                last.path("commit").path("author").path("name").asText(null)));
    }

    @Override
    public Optional<String> headCommit(String installationExternalId, ForgeRef ref) {
        String path = "/repos/%s/commits/%s".formatted(ref.repositoryFullName(), encode(ref.ref()));
        return getAsInstallation(installationExternalId, path).map(commit -> commit.path("sha").asText(null));
    }

    @Override
    public List<String> changedFiles(String installationExternalId, String repositoryFullName, int pullRequestNumber) {
        List<String> paths = new ArrayList<>();
        for (int page = 1; ; page++) {
            String path = "/repos/%s/pulls/%d/files?per_page=100&page=%d"
                    .formatted(repositoryFullName, pullRequestNumber, page);
            JsonNode files = getAsInstallation(installationExternalId, path).orElse(json.createArrayNode());
            if (!files.isArray() || files.isEmpty()) {
                break;
            }
            for (JsonNode file : files) {
                paths.add(file.path("filename").asText());
            }
            if (files.size() < 100) {
                break;
            }
        }
        return List.copyOf(paths);
    }

    @Override
    public void reportReviewStatus(
            String installationExternalId,
            String repositoryFullName,
            String commitSha,
            ReviewStatusState state,
            String description,
            String targetUrl) {
        Map<String, String> body = new java.util.LinkedHashMap<>();
        body.put("state", switch (state) {
            case PENDING -> "pending";
            case SUCCESS -> "success";
            case FAILURE -> "failure";
        });
        body.put("context", properties.statusContext());
        if (description != null) {
            // GitHub truncates a status description at 140 characters and rejects nothing, so a
            // long review summary would be silently cut; trim it here where it is visible.
            body.put("description", description.length() > 140 ? description.substring(0, 140) : description);
        }
        if (targetUrl != null) {
            body.put("target_url", targetUrl);
        }
        send(HttpRequest.newBuilder()
                .uri(uri("/repos/%s/statuses/%s".formatted(repositoryFullName, commitSha)))
                .header("Authorization", "Bearer " + installationToken(installationExternalId))
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(write(body)))
                .timeout(TIMEOUT)
                .build());
    }

    private Optional<JsonNode> getAsApp(String path) {
        return get(path, appJwt());
    }

    private Optional<JsonNode> getAsInstallation(String installationExternalId, String path) {
        return get(path, installationToken(installationExternalId));
    }

    private Optional<JsonNode> get(String path, String token) {
        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(TIMEOUT)
                .GET()
                .build());
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        return Optional.of(read(response.body()));
    }

    private String installationToken(String installationExternalId) {
        InstallationToken cached = tokens.get(installationExternalId);
        Instant now = clock.instant();
        if (cached != null && cached.expiresAt().isAfter(now.plus(TOKEN_EARLY_EXPIRY))) {
            return cached.token();
        }
        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(uri("/app/installations/%s/access_tokens".formatted(installationExternalId)))
                .header("Authorization", "Bearer " + appJwt())
                .header("Accept", "application/vnd.github+json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build());
        JsonNode body = read(response.body());
        String token = body.path("token").asText(null);
        if (token == null) {
            throw new ForgeException("GitHub returned no installation token for " + installationExternalId);
        }
        Instant expiresAt = body.hasNonNull("expires_at")
                ? Instant.parse(body.path("expires_at").asText())
                : now.plus(Duration.ofMinutes(10));
        tokens.put(installationExternalId, new InstallationToken(token, expiresAt));
        return token;
    }

    private String appJwt() {
        if (!properties.configured()) {
            throw new ForgeException("No GitHub App is configured; set specforge.github.app-id and private-key.");
        }
        Instant now = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.appId())
                // GitHub rejects a JWT issued in its own future, so back-date it against clock skew.
                .issueTime(Date.from(now.minusSeconds(60)))
                .expirationTime(Date.from(now.plusSeconds(540)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        try {
            jwt.sign(new RSASSASigner(privateKey()));
        } catch (JOSEException e) {
            throw new ForgeException("Could not sign the GitHub App JWT", e);
        }
        return jwt.serialize();
    }

    private java.security.PrivateKey privateKey() {
        String pem = properties.privateKey()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            // A PKCS#1 key ("BEGIN RSA PRIVATE KEY") is what GitHub hands out by default and is not
            // what this expects; saying so beats a bare InvalidKeySpecException in the log.
            throw new ForgeException("The GitHub App private key must be PKCS#8 PEM", e);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400 && response.statusCode() != 404) {
                throw new ForgeException("GitHub answered %d for %s".formatted(response.statusCode(), request.uri().getPath()));
            }
            return response;
        } catch (IOException e) {
            throw new ForgeException("Could not reach GitHub at " + request.uri().getPath(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ForgeException("Interrupted while calling GitHub", e);
        }
    }

    private URI uri(String path) {
        return URI.create(properties.apiBaseUrl() + path);
    }

    private JsonNode read(String body) {
        try {
            return json.readTree(body);
        } catch (JacksonException e) {
            throw new ForgeException("GitHub returned a body that is not JSON", e);
        }
    }

    private String write(Object body) {
        return json.writeValueAsString(body);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Path segments keep their slashes; only the segments themselves are escaped. */
    private static String encodePath(String path) {
        StringBuilder encoded = new StringBuilder(path.length());
        for (String segment : path.split("/", -1)) {
            if (!encoded.isEmpty()) {
                encoded.append('/');
            }
            encoded.append(encode(segment));
        }
        return encoded.toString();
    }

    private record InstallationToken(String token, Instant expiresAt) {}
}
