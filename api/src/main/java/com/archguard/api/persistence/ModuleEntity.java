package com.archguard.api.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/** One Java package in a persisted scan graph. */
@Entity
@Table(name = "modules")
public class ModuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private ScanEntity scan;

    private String name;

    protected ModuleEntity() {
    }

    public ModuleEntity(ScanEntity scan, String name) { this.scan = scan; this.name = name; }
    public UUID getId() { return id; }
    public ScanEntity getScan() { return scan; }
    public String getName() { return name; }
}
