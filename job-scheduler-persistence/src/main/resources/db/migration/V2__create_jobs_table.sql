-- Features #1, #4, #5, #6, #7, #8, #9
-- Create jobs table and related tables

CREATE TABLE jobs (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    type VARCHAR(100) NOT NULL,
    job_group VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    version INTEGER NOT NULL DEFAULT 1,
    configuration JSONB,
    parameters JSONB,
    schedule_id VARCHAR(64),
    retry_max_attempts INTEGER,
    retry_initial_delay_ms BIGINT,
    retry_max_delay_ms BIGINT,
    retry_backoff_multiplier DOUBLE PRECISION,
    retry_strategy VARCHAR(50),
    retry_on_timeout BOOLEAN,
    timeout_ms BIGINT,
    max_concurrent_executions INTEGER DEFAULT 1,
    tenant_id VARCHAR(100),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT uk_job_name_tenant UNIQUE (name, tenant_id),
    CONSTRAINT fk_job_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE SET NULL
);

-- Job dependencies table
CREATE TABLE job_dependencies (
    job_id VARCHAR(64) NOT NULL,
    dependency_id VARCHAR(64) NOT NULL,

    PRIMARY KEY (job_id, dependency_id),
    CONSTRAINT fk_job_dependency_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

-- Job tags table
CREATE TABLE job_tags (
    job_id VARCHAR(64) NOT NULL,
    tag VARCHAR(100) NOT NULL,

    PRIMARY KEY (job_id, tag),
    CONSTRAINT fk_job_tag_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

-- Indexes for job queries
CREATE INDEX idx_job_status ON jobs(status);
CREATE INDEX idx_job_tenant ON jobs(tenant_id);
CREATE INDEX idx_job_group ON jobs(job_group);
CREATE INDEX idx_job_type ON jobs(type);
CREATE INDEX idx_job_enabled ON jobs(enabled);
CREATE INDEX idx_job_created_at ON jobs(created_at);
CREATE INDEX idx_job_priority ON jobs(priority);
CREATE INDEX idx_job_dependency ON job_dependencies(job_id, dependency_id);
CREATE INDEX idx_job_tag ON job_tags(tag);

-- GIN indexes for JSONB columns (PostgreSQL-specific)
CREATE INDEX idx_job_configuration_gin ON jobs USING GIN(configuration);
CREATE INDEX idx_job_parameters_gin ON jobs USING GIN(parameters);
CREATE INDEX idx_job_metadata_gin ON jobs USING GIN(metadata);

-- Comments
COMMENT ON TABLE jobs IS 'Job definitions and configurations';
COMMENT ON COLUMN jobs.type IS 'Job type: shell, http, java, custom, etc.';
COMMENT ON COLUMN jobs.status IS 'Job status: SCHEDULED, RUNNING, SUCCESS, FAILED, CANCELLED, TIMEOUT, PAUSED, WAITING, RETRYING, UNKNOWN';
COMMENT ON COLUMN jobs.priority IS 'Job priority: LOWEST, LOW, NORMAL, HIGH, HIGHEST, CRITICAL';
COMMENT ON COLUMN jobs.version IS 'Job version for optimistic locking';
COMMENT ON COLUMN jobs.configuration IS 'Job configuration as JSON';
COMMENT ON COLUMN jobs.parameters IS 'Job parameters as JSON';
COMMENT ON COLUMN jobs.metadata IS 'Job metadata as JSON';
COMMENT ON COLUMN jobs.tenant_id IS 'Tenant identifier for multi-tenancy';
COMMENT ON TABLE job_dependencies IS 'Job dependency relationships';
COMMENT ON TABLE job_tags IS 'Job tags for categorization';
