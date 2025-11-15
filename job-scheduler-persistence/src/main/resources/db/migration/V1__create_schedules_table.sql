-- Feature #3: Advanced Scheduling
-- Create schedules table

CREATE TABLE schedules (
    id VARCHAR(64) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    cron_expression VARCHAR(255),
    interval_ms BIGINT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    timezone VARCHAR(100),
    max_executions INTEGER,
    execution_count INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    last_execution_time TIMESTAMP,
    next_execution_time TIMESTAMP,
    misfire_strategy VARCHAR(50) DEFAULT 'FIRE_ONCE'
);

-- Indexes for schedule queries
CREATE INDEX idx_schedule_active ON schedules(active);
CREATE INDEX idx_schedule_next_execution ON schedules(next_execution_time);

-- Comments
COMMENT ON TABLE schedules IS 'Job scheduling configuration';
COMMENT ON COLUMN schedules.type IS 'Schedule type: CRON, INTERVAL, FIXED_RATE, ONE_TIME, MANUAL, EVENT_DRIVEN, DEPENDENCY_BASED';
COMMENT ON COLUMN schedules.cron_expression IS 'Cron expression for CRON type schedules';
COMMENT ON COLUMN schedules.interval_ms IS 'Interval in milliseconds for INTERVAL type schedules';
COMMENT ON COLUMN schedules.misfire_strategy IS 'Strategy for handling missed executions: FIRE_ONCE, FIRE_ALL, IGNORE, FIRE_NEXT';
