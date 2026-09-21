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

/** One directed edge in a persisted scan graph. */
@Entity
@Table(name = "dependencies")
public class DependencyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "scan_id", nullable = false)
    private ScanEntity scan;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "from_module_id", nullable = false)
    private ModuleEntity fromModule;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "to_module_id", nullable = false)
    private ModuleEntity toModule;

    protected DependencyEntity() { }
    public DependencyEntity(ScanEntity scan, ModuleEntity fromModule, ModuleEntity toModule) {
        this.scan = scan; this.fromModule = fromModule; this.toModule = toModule;
    }
    public UUID getId() { return id; }
    public ModuleEntity getFromModule() { return fromModule; }
    public ModuleEntity getToModule() { return toModule; }
}
