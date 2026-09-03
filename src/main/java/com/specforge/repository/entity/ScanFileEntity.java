package com.specforge.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** One file a scan looked at, with the verdict the wizard shows against that path. */
@Entity
@Table(name = "repository_scan_file")
public class ScanFileEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "scan_id", nullable = false)
    private UUID scanId;

    @Column(name = "path", nullable = false, length = 1024)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", nullable = false, length = 32)
    private Classification classification;

    @Column(name = "reason", length = 1024)
    private String reason;

    protected ScanFileEntity() {
        // for JPA
    }

    public ScanFileEntity(UUID id, UUID scanId, String path, Classification classification, String reason) {
        this.id = id;
        this.scanId = scanId;
        this.path = path;
        this.classification = classification;
        this.reason = reason;
    }

    public UUID id() {
        return id;
    }

    public UUID scanId() {
        return scanId;
    }

    public String path() {
        return path;
    }

    public Classification classification() {
        return classification;
    }

    public String reason() {
        return reason;
    }
}
