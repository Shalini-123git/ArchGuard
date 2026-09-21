package com.archguard.api.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** A deterministic architecture-rule violation and its persisted blast radius. */
@Entity
@Table(name = "violations")
public class ViolationEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "scan_id", nullable = false)
    private ScanEntity scan;
    private String ruleId;
    private String severity;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_module_id")
    private ModuleEntity sourceModule;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "target_module_id")
    private ModuleEntity targetModule;
    private int blastRadiusCount;
    @ManyToMany
    @JoinTable(name = "violation_affected_modules", joinColumns = @JoinColumn(name = "violation_id"), inverseJoinColumns = @JoinColumn(name = "module_id"))
    private Set<ModuleEntity> affectedModules = new LinkedHashSet<>();

    protected ViolationEntity() { }
    public ViolationEntity(ScanEntity scan, String ruleId, String severity, ModuleEntity sourceModule, ModuleEntity targetModule, Set<ModuleEntity> affectedModules) {
        this.scan = scan; this.ruleId = ruleId; this.severity = severity; this.sourceModule = sourceModule; this.targetModule = targetModule;
        this.affectedModules.addAll(affectedModules); this.blastRadiusCount = affectedModules.size();
    }
    public UUID getId() { return id; }
    public String getRuleId() { return ruleId; }
    public String getSeverity() { return severity; }
    public ModuleEntity getSourceModule() { return sourceModule; }
    public ModuleEntity getTargetModule() { return targetModule; }
    public int getBlastRadiusCount() { return blastRadiusCount; }
    public Set<ModuleEntity> getAffectedModules() { return Set.copyOf(affectedModules); }
}
