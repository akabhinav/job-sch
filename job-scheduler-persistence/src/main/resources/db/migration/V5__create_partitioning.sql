-- Feature #22: Job Execution History - Advanced partitioning for large datasets
-- This script sets up table partitioning for job_executions to improve query performance

-- Note: PostgreSQL partitioning is optional and can be enabled for high-volume deployments
-- This creates a partitioned version of job_executions table by month

-- Create partitioned job executions table (optional, for high-volume scenarios)
-- Uncomment and use this if you need partitioning

/*
-- Rename existing table
ALTER TABLE job_executions RENAME TO job_executions_old;

-- Create new partitioned table
CREATE TABLE job_executions (
    id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64) NOT NULL,
    job_name VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_ms BIGINT,
    trigger_type VARCHAR(50),
    triggered_by VARCHAR(100),
    parameters JSONB,
    context JSONB,
    result JSONB,
    error_message VARCHAR(2000),
    stack_trace TEXT,
    attempt_number INTEGER NOT NULL DEFAULT 1,
    node_id VARCHAR(100),
    process_id VARCHAR(100),
    thread_name VARCHAR(255),
    logs TEXT,
    log_reference VARCHAR(500),
    metrics JSONB,
    tenant_id VARCHAR(100),

    PRIMARY KEY (id, start_time)
) PARTITION BY RANGE (start_time);

-- Create partitions for current and next 6 months
CREATE TABLE job_executions_2025_01 PARTITION OF job_executions
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
CREATE TABLE job_executions_2025_02 PARTITION OF job_executions
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
CREATE TABLE job_executions_2025_03 PARTITION OF job_executions
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');
CREATE TABLE job_executions_2025_04 PARTITION OF job_executions
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');
CREATE TABLE job_executions_2025_05 PARTITION OF job_executions
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');
CREATE TABLE job_executions_2025_06 PARTITION OF job_executions
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');

-- Copy data from old table
INSERT INTO job_executions SELECT * FROM job_executions_old;

-- Drop old table
DROP TABLE job_executions_old;

-- Recreate indexes on partitioned table
CREATE INDEX idx_execution_job_id ON job_executions(job_id);
CREATE INDEX idx_execution_status ON job_executions(status);
CREATE INDEX idx_execution_start_time ON job_executions(start_time);
CREATE INDEX idx_execution_end_time ON job_executions(end_time);
CREATE INDEX idx_execution_tenant ON job_executions(tenant_id);
CREATE INDEX idx_execution_node ON job_executions(node_id);
CREATE INDEX idx_execution_job_start ON job_executions(job_id, start_time);
CREATE INDEX idx_execution_parameters_gin ON job_executions USING GIN(parameters);
CREATE INDEX idx_execution_context_gin ON job_executions USING GIN(context);
CREATE INDEX idx_execution_metrics_gin ON job_executions USING GIN(metrics);
*/

-- For now, just add a comment about partitioning strategy
COMMENT ON TABLE job_executions IS 'Job execution history and results. Consider partitioning by start_time for high-volume deployments.';
