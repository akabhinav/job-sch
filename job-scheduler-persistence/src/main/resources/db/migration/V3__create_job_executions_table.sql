-- Features #2, #4, #17, #22, #23
-- Create job executions table

CREATE TABLE job_executions (
    id VARCHAR(64) PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    job_name VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    start_time TIMESTAMP,
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

    CONSTRAINT fk_execution_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

-- Indexes for execution queries
CREATE INDEX idx_execution_job_id ON job_executions(job_id);
CREATE INDEX idx_execution_status ON job_executions(status);
CREATE INDEX idx_execution_start_time ON job_executions(start_time);
CREATE INDEX idx_execution_end_time ON job_executions(end_time);
CREATE INDEX idx_execution_tenant ON job_executions(tenant_id);
CREATE INDEX idx_execution_node ON job_executions(node_id);
CREATE INDEX idx_execution_job_start ON job_executions(job_id, start_time);

-- GIN indexes for JSONB columns (PostgreSQL-specific)
CREATE INDEX idx_execution_parameters_gin ON job_executions USING GIN(parameters);
CREATE INDEX idx_execution_context_gin ON job_executions USING GIN(context);
CREATE INDEX idx_execution_metrics_gin ON job_executions USING GIN(metrics);

-- Comments
COMMENT ON TABLE job_executions IS 'Job execution history and results';
COMMENT ON COLUMN job_executions.status IS 'Execution status: SCHEDULED, RUNNING, SUCCESS, FAILED, CANCELLED, TIMEOUT, WAITING, RETRYING';
COMMENT ON COLUMN job_executions.trigger_type IS 'How execution was triggered: SCHEDULED, MANUAL, DEPENDENCY, EVENT, RETRY, WEBHOOK, API';
COMMENT ON COLUMN job_executions.parameters IS 'Execution parameters as JSON';
COMMENT ON COLUMN job_executions.context IS 'Execution context for data passing as JSON';
COMMENT ON COLUMN job_executions.result IS 'Execution result as JSON';
COMMENT ON COLUMN job_executions.metrics IS 'Execution metrics as JSON';
COMMENT ON COLUMN job_executions.duration_ms IS 'Execution duration in milliseconds';
