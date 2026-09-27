CREATE TABLE repositories (
    id UUID PRIMARY KEY,
    repository_url VARCHAR(2048) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE scans (
    id UUID PRIMARY KEY,
    repository_id UUID NOT NULL REFERENCES repositories(id),
    source_type VARCHAR(16) NOT NULL,
    commit_sha VARCHAR(64),
    rules_yaml TEXT,
    status VARCHAR(16) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_scans_repository_created_at ON scans(repository_id, created_at);
CREATE INDEX idx_scans_status ON scans(status);

CREATE TABLE modules (
    id UUID PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES scans(id) ON DELETE CASCADE,
    name VARCHAR(1024) NOT NULL,
    CONSTRAINT uq_modules_scan_name UNIQUE (scan_id, name)
);

CREATE TABLE dependencies (
    id UUID PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES scans(id) ON DELETE CASCADE,
    from_module_id UUID NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    to_module_id UUID NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    CONSTRAINT uq_dependencies_scan_edge UNIQUE (scan_id, from_module_id, to_module_id)
);
CREATE INDEX idx_dependencies_scan ON dependencies(scan_id);

CREATE TABLE violations (
    id UUID PRIMARY KEY,
    scan_id UUID NOT NULL REFERENCES scans(id) ON DELETE CASCADE,
    rule_id VARCHAR(255) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    source_module_id UUID REFERENCES modules(id) ON DELETE SET NULL,
    target_module_id UUID REFERENCES modules(id) ON DELETE SET NULL,
    blast_radius_count INTEGER NOT NULL
);
CREATE INDEX idx_violations_scan ON violations(scan_id);

CREATE TABLE violation_affected_modules (
    violation_id UUID NOT NULL REFERENCES violations(id) ON DELETE CASCADE,
    module_id UUID NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    PRIMARY KEY (violation_id, module_id)
);
