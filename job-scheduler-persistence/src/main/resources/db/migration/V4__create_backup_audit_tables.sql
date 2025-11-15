-- Feature #63: Backup and Restore
-- Create audit and backup-related tables

-- Audit log for tracking changes
CREATE TABLE job_audit_log (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    action VARCHAR(50) NOT NULL,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_values JSONB,
    new_values JSONB,
    tenant_id VARCHAR(100),

    CONSTRAINT fk_audit_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

-- Index for audit queries
CREATE INDEX idx_audit_job_id ON job_audit_log(job_id);
CREATE INDEX idx_audit_changed_at ON job_audit_log(changed_at);
CREATE INDEX idx_audit_action ON job_audit_log(action);
CREATE INDEX idx_audit_tenant ON job_audit_log(tenant_id);

-- Backup metadata table
CREATE TABLE backup_metadata (
    id BIGSERIAL PRIMARY KEY,
    backup_name VARCHAR(255) NOT NULL,
    backup_type VARCHAR(50) NOT NULL,
    backup_path VARCHAR(1000),
    backup_size_bytes BIGINT,
    job_count INTEGER,
    execution_count INTEGER,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    completed_at TIMESTAMP,
    error_message TEXT,
    tenant_id VARCHAR(100),
    metadata JSONB
);

-- Index for backup queries
CREATE INDEX idx_backup_created_at ON backup_metadata(created_at);
CREATE INDEX idx_backup_status ON backup_metadata(status);
CREATE INDEX idx_backup_tenant ON backup_metadata(tenant_id);

-- Comments
COMMENT ON TABLE job_audit_log IS 'Audit trail for job changes';
COMMENT ON COLUMN job_audit_log.action IS 'Action performed: CREATE, UPDATE, DELETE, EXECUTE, PAUSE, RESUME';
COMMENT ON TABLE backup_metadata IS 'Backup operation metadata';
COMMENT ON COLUMN backup_metadata.backup_type IS 'Type of backup: FULL, INCREMENTAL, JOBS_ONLY, EXECUTIONS_ONLY';
COMMENT ON COLUMN backup_metadata.status IS 'Backup status: PENDING, IN_PROGRESS, COMPLETED, FAILED';
