package com.specforge;

import com.specforge.repository.internal.forge.Forge;
import com.specforge.repository.internal.forge.ForgeFile;
import com.specforge.repository.internal.forge.ForgeInstallationInfo;
import com.specforge.repository.internal.forge.ForgeRef;
import com.specforge.repository.internal.forge.ForgeRepositoryInfo;
import com.specforge.repository.internal.forge.ReviewStatusState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A repository SpecForge can read without a network. The integration suite drives the real import,
 * sync and degradation paths against this: the thing under test is what SpecForge does with what a
 * forge says, not whether GitHub's API is reachable from CI.
 */
public class FakeForge implements Forge {

    public record StatusCall(String repositoryFullName, String commitSha, ReviewStatusState state) {}

    private final Map<String, String> files = new LinkedHashMap<>();
    private final Map<Integer, List<String>> pullRequestFiles = new LinkedHashMap<>();
    private final List<StatusCall> statuses = new ArrayList<>();

    private String installationExternalId = "4711";
    private String accountLogin = "acme";
    private String repositoryFullName = "acme/billing-specs";
    private boolean installed = true;
    private boolean suspended = false;
    private String headCommit = "c0ffee1";
    private String author = "ada";

    public void reset() {
        files.clear();
        pullRequestFiles.clear();
        statuses.clear();
        installed = true;
        suspended = false;
        headCommit = "c0ffee1";
    }

    public void put(String path, String content) {
        files.put(path, content);
    }

    public void remove(String path) {
        files.remove(path);
    }

    public void commit(String sha) {
        headCommit = sha;
    }

    public void pullRequest(int number, List<String> paths) {
        pullRequestFiles.put(number, List.copyOf(paths));
    }

    public void uninstall() {
        installed = false;
    }

    public void suspend(boolean suspended) {
        this.suspended = suspended;
    }

    public List<StatusCall> statuses() {
        return List.copyOf(statuses);
    }

    public String installationId() {
        return installationExternalId;
    }

    public String repository() {
        return repositoryFullName;
    }

    public String head() {
        return headCommit;
    }

    @Override
    public Optional<ForgeInstallationInfo> installation(String externalId) {
        if (!installed || !externalId.equals(installationExternalId)) {
            return Optional.empty();
        }
        return Optional.of(new ForgeInstallationInfo(
                externalId,
                accountLogin,
                "Organization",
                23,
                suspended,
                List.of(new ForgeRepositoryInfo(repositoryFullName, "main", "9001"))));
    }

    @Override
    public List<String> listFiles(String externalId, ForgeRef ref) {
        return List.copyOf(files.keySet());
    }

    @Override
    public Optional<ForgeFile> readFile(String externalId, ForgeRef ref, String path) {
        return Optional.ofNullable(files.get(path))
                .map(content -> new ForgeFile(path, content, headCommit, author));
    }

    @Override
    public Optional<String> headCommit(String externalId, ForgeRef ref) {
        return Optional.of(headCommit);
    }

    @Override
    public List<String> changedFiles(String externalId, String repository, int pullRequestNumber) {
        return pullRequestFiles.getOrDefault(pullRequestNumber, List.of());
    }

    @Override
    public void reportReviewStatus(
            String externalId,
            String repository,
            String commitSha,
            ReviewStatusState state,
            String description,
            String targetUrl) {
        statuses.add(new StatusCall(repository, commitSha, state));
    }
}
