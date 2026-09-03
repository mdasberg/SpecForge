package com.specforge.repository.service;

import com.specforge.platform.api.dto.Connection;
import com.specforge.platform.api.dto.ForgeInstallation;
import com.specforge.platform.api.dto.ForgeProvider;
import com.specforge.platform.api.dto.GrantedRepository;
import com.specforge.platform.api.dto.ImportOutcome;
import com.specforge.platform.api.dto.ImportRun;
import com.specforge.platform.api.dto.ImportRunFile;
import com.specforge.platform.api.dto.Scan;
import com.specforge.platform.api.dto.ScanClassification;
import com.specforge.platform.api.dto.ScanFile;
import com.specforge.platform.api.dto.SpecFormat;
import com.specforge.platform.api.dto.SyncMode;
import com.specforge.repository.entity.ForgeInstallationEntity;
import com.specforge.repository.entity.ImportRunEntity;
import com.specforge.repository.entity.ImportRunFileEntity;
import com.specforge.repository.entity.RepositoryConnectionEntity;
import com.specforge.repository.entity.RepositoryScanEntity;
import com.specforge.repository.entity.ScanFileEntity;
import com.specforge.repository.entity.SpecFileFormat;
import com.specforge.repository.entity.SyncPolicy;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;


/**
 * Entities to contract types. The domain enums and the generated ones are separate on purpose —
 * the contract is allowed to be renamed without a migration, and the database is allowed to hold a
 * value the contract has not published yet — so they are mapped by name here rather than shared.
 */
final class RepositoryMapper {

    private RepositoryMapper() {}

    static SpecFileFormat format(SpecFormat format) {
        return SpecFileFormat.valueOf(format.name());
    }

    static SyncPolicy syncPolicy(SyncMode syncMode) {
        return SyncPolicy.valueOf(syncMode.name());
    }

    static Connection connection(RepositoryConnectionEntity entity, String projectName) {
        Connection connection = new Connection(
                entity.id(),
                projectName,
                entity.installationId(),
                entity.repositoryFullName(),
                entity.branch(),
                entity.pathGlob(),
                SpecFormat.valueOf(entity.specFormat().name()),
                SyncMode.valueOf(entity.syncMode().name()),
                Connection.StateEnum.valueOf(entity.state().name()),
                at(entity.createdAt()));
        connection.setDegradedReason(entity.degradedReason());
        return connection;
    }

    static ForgeInstallation installation(ForgeInstallationEntity entity) {
        List<GrantedRepository> repositories = entity.repositories().stream()
                .map(repository -> {
                    GrantedRepository granted = new GrantedRepository(repository.fullName());
                    granted.setDefaultBranch(repository.defaultBranch());
                    granted.setExternalId(repository.externalId());
                    return granted;
                })
                .toList();
        ForgeInstallation installation = new ForgeInstallation(
                entity.id(),
                ForgeProvider.valueOf(entity.provider()),
                entity.accountLogin(),
                ForgeInstallation.StatusEnum.valueOf(entity.status().name()),
                repositories.size(),
                repositories);
        installation.setAccountType(entity.accountType());
        installation.setAccountRepositoryCount(entity.accountRepositoryCount());
        return installation;
    }

    static Scan scan(RepositoryScanEntity entity, List<ScanFileEntity> files) {
        Scan scan = new Scan(
                entity.id(),
                Scan.StatusEnum.valueOf(entity.status().name()),
                entity.repositoryFullName(),
                entity.branch(),
                entity.pathGlob(),
                SpecFormat.valueOf(entity.specFormat().name()),
                entity.importableCount(),
                entity.changeProposalCount(),
                entity.unparsableCount(),
                at(entity.startedAt()),
                files.stream().map(RepositoryMapper::scanFile).toList());
        scan.setFailureReason(entity.failureReason());
        scan.setFinishedAt(at(entity.finishedAt()));
        return scan;
    }

    static ImportRun importRun(ImportRunEntity entity, List<ImportRunFileEntity> files) {
        ImportRun run = new ImportRun(
                entity.id(),
                entity.connectionId(),
                ImportRun.TriggerEnum.valueOf(entity.trigger().name()),
                ImportRun.StatusEnum.valueOf(entity.status().name()),
                at(entity.startedAt()),
                entity.importedCount(),
                entity.unchangedCount(),
                entity.skippedCount(),
                entity.failedCount(),
                files.stream().map(RepositoryMapper::importRunFile).toList());
        run.setCommitSha(entity.commitSha());
        run.setFinishedAt(at(entity.finishedAt()));
        return run;
    }

    private static ScanFile scanFile(ScanFileEntity entity) {
        ScanFile file = new ScanFile(entity.path(), ScanClassification.valueOf(entity.classification().name()));
        file.setReason(entity.reason());
        return file;
    }

    private static ImportRunFile importRunFile(ImportRunFileEntity entity) {
        ImportRunFile file = new ImportRunFile(entity.path(), ImportOutcome.valueOf(entity.outcome().name()));
        file.setReason(entity.reason());
        return file;
    }

    private static OffsetDateTime at(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
