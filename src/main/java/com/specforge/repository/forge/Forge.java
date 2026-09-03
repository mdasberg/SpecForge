package com.specforge.repository.forge;

import java.util.List;
import java.util.Optional;

/**
 * Everything SpecForge is allowed to do to a source forge. The port is deliberately almost
 * entirely read-only: there is no operation here that creates a commit, a branch, a pull request
 * or a comment, because specification content lives in the repository and SpecForge only mirrors
 * it. {@link #reportReviewStatus} is the single outbound write, and it writes a signal on a commit
 * rather than content.
 *
 * <p>An architecture test asserts both halves of that: no module outside {@code repository} may
 * reference this port, and this port may not grow a content-write operation.
 */
public interface Forge {

    /** The installation as the forge currently reports it, or empty when it no longer exists. */
    public Optional<ForgeInstallationInfo> installation(String installationExternalId);

    /** Every file path on a ref, from which the connection's glob selects the specifications. */
    public List<String> listFiles(String installationExternalId, ForgeRef ref);

    /** One file's content at a ref, with the commit that last touched it. */
    public Optional<ForgeFile> readFile(String installationExternalId, ForgeRef ref, String path);

    /** The commit a branch currently points at. */
    public Optional<String> headCommit(String installationExternalId, ForgeRef ref);

    /** The paths a pull request changes, so a sync only re-imports what actually moved. */
    public List<String> changedFiles(String installationExternalId, String repositoryFullName, int pullRequestNumber);

    /**
     * Posts the state of a SpecForge review onto a commit. The only write this port permits: it
     * reports a verdict that already exists, and never touches repository content.
     */
    public void reportReviewStatus(
            String installationExternalId,
            String repositoryFullName,
            String commitSha,
            ReviewStatusState state,
            String description,
            String targetUrl);
}
