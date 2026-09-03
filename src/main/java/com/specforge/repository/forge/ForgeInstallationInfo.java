package com.specforge.repository.forge;

import java.util.List;

/**
 * An installation and the repositories it grants. {@code accountRepositoryCount} is what lets the
 * wizard say "4 of 23 granted" rather than only naming the four; it is null when the forge does
 * not report the account total.
 */
public record ForgeInstallationInfo(
        String externalId,
        String accountLogin,
        String accountType,
        Integer accountRepositoryCount,
        boolean suspended,
        List<ForgeRepositoryInfo> repositories) {}
