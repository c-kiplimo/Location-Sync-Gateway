-- V1__create_initial_schema.sql
-- Initial database schema for Location Sync Gateway

-- Create custom types
CREATE TYPE resolution_outcome_type AS ENUM ('AUTO_ACCEPT', 'AUTO_REVERT', 'FLAG_FOR_REVIEW');
CREATE TYPE data_source_type AS ENUM ('GOOGLE_MY_BUSINESS', 'POS_SYSTEM', 'ERP_SYSTEM', 'WEBHOOK');
CREATE TYPE field_type_enum AS ENUM ('PHONE', 'OPENING_HOURS', 'COORDINATES', 'CATEGORIES', 'ADDRESS', 'TEMPORARY_CLOSURE', 'WEBSITE', 'DESCRIPTION');
CREATE TYPE severity_type AS ENUM ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW');

-- Conflicts table
CREATE TABLE conflicts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id BIGINT NOT NULL,
    field_type field_type_enum NOT NULL,
    source data_source_type NOT NULL,
    external_value JSONB,
    uberall_value JSONB,
    confidence DECIMAL(3,2) CHECK (confidence >= 0 AND confidence <= 1),
    quality_score JSONB,
    anomaly_score DECIMAL(3,2),
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for conflicts
CREATE INDEX idx_conflicts_location_id ON conflicts(location_id);
CREATE INDEX idx_conflicts_detected_at ON conflicts(detected_at DESC);
CREATE INDEX idx_conflicts_source ON conflicts(source);
CREATE INDEX idx_conflicts_field_type ON conflicts(field_type);
CREATE INDEX idx_conflicts_created_at ON conflicts(created_at DESC);
CREATE INDEX idx_conflicts_composite ON conflicts(location_id, detected_at DESC);

-- Resolution decisions table
CREATE TABLE resolution_decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conflict_id UUID NOT NULL REFERENCES conflicts(id) ON DELETE CASCADE,
    location_id BIGINT NOT NULL,
    field_type field_type_enum NOT NULL,
    source data_source_type NOT NULL,
    outcome resolution_outcome_type NOT NULL,
    rule_applied VARCHAR(200),
    ml_prediction JSONB,
    explanation TEXT,
    confidence DECIMAL(3,2),
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL,
    executed_at TIMESTAMP WITH TIME ZONE,
    execution_status VARCHAR(50),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for resolution_decisions
CREATE INDEX idx_decisions_conflict_id ON resolution_decisions(conflict_id);
CREATE INDEX idx_decisions_location_id ON resolution_decisions(location_id);
CREATE INDEX idx_decisions_outcome ON resolution_decisions(outcome);
CREATE INDEX idx_decisions_decided_at ON resolution_decisions(decided_at DESC);
CREATE INDEX idx_decisions_execution_status ON resolution_decisions(execution_status);

-- Location snapshots table (for historical tracking)
CREATE TABLE location_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id BIGINT NOT NULL,
    source data_source_type NOT NULL,
    snapshot_data JSONB NOT NULL,
    quality_score JSONB,
    captured_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for location_snapshots
CREATE INDEX idx_snapshots_location_id ON location_snapshots(location_id);
CREATE INDEX idx_snapshots_source ON location_snapshots(source);
CREATE INDEX idx_snapshots_captured_at ON location_snapshots(captured_at DESC);
CREATE INDEX idx_snapshots_composite ON location_snapshots(location_id, captured_at DESC);

-- Resolution rules table
CREATE TABLE resolution_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL UNIQUE,
    field_type field_type_enum NOT NULL,
    source data_source_type,
    location_pattern VARCHAR(500),
    default_outcome resolution_outcome_type NOT NULL,
    minimum_confidence DECIMAL(3,2) DEFAULT 0.9,
    conditions JSONB,
    priority INT DEFAULT 100,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Indexes for resolution_rules
CREATE INDEX idx_rules_field_type ON resolution_rules(field_type);
CREATE INDEX idx_rules_source ON resolution_rules(source);
CREATE INDEX idx_rules_enabled ON resolution_rules(enabled);
CREATE INDEX idx_rules_priority ON resolution_rules(priority);

-- Audit log table
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    payload JSONB,
    performed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    performed_by VARCHAR(100),
    correlation_id UUID,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for audit_log
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_correlation ON audit_log(correlation_id);
CREATE INDEX idx_audit_performed_at ON audit_log(performed_at DESC);
CREATE INDEX idx_audit_action ON audit_log(action);

-- OAuth tokens table
CREATE TABLE oauth_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source data_source_type NOT NULL,
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    token_type VARCHAR(50),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    scope TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT unique_source UNIQUE (source)
);

-- Indexes for oauth_tokens
CREATE INDEX idx_oauth_tokens_source ON oauth_tokens(source);
CREATE INDEX idx_oauth_tokens_expires_at ON oauth_tokens(expires_at);

-- ML model metadata table
CREATE TABLE ml_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_name VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    algorithm VARCHAR(100),
    trained_at TIMESTAMP WITH TIME ZONE,
    training_samples INTEGER,
    metrics JSONB,
    features JSONB,
    deployed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    model_path TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT unique_model_version UNIQUE (model_name, version)
);

-- Indexes for ml_models
CREATE INDEX idx_ml_models_name ON ml_models(model_name);
CREATE INDEX idx_ml_models_status ON ml_models(status);
CREATE INDEX idx_ml_models_deployed_at ON ml_models(deployed_at DESC);

-- Quality baselines table (for consistency validation)
CREATE TABLE quality_baselines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id BIGINT NOT NULL,
    baseline_data JSONB NOT NULL,
    quality_score JSONB,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sample_count INTEGER DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT unique_location UNIQUE (location_id)
);

-- Indexes for quality_baselines
CREATE INDEX idx_baselines_location_id ON quality_baselines(location_id);
CREATE INDEX idx_baselines_calculated_at ON quality_baselines(calculated_at DESC);

-- Idempotency keys table
CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(500) NOT NULL UNIQUE,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    response JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Indexes for idempotency_keys
CREATE INDEX idx_idempotency_key ON idempotency_keys(idempotency_key);
CREATE INDEX idx_idempotency_expires_at ON idempotency_keys(expires_at);

-- Notification queue table
CREATE TABLE notification_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conflict_id UUID REFERENCES conflicts(id) ON DELETE CASCADE,
    decision_id UUID REFERENCES resolution_decisions(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(200),
    subject VARCHAR(500),
    message TEXT NOT NULL,
    metadata JSONB,
    status VARCHAR(50) DEFAULT 'PENDING',
    attempts INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 3,
    sent_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for notification_queue
CREATE INDEX idx_notifications_status ON notification_queue(status);
CREATE INDEX idx_notifications_created_at ON notification_queue(created_at DESC);
CREATE INDEX idx_notifications_conflict_id ON notification_queue(conflict_id);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at triggers
CREATE TRIGGER update_conflicts_updated_at BEFORE UPDATE ON conflicts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_decisions_updated_at BEFORE UPDATE ON resolution_decisions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rules_updated_at BEFORE UPDATE ON resolution_rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_oauth_tokens_updated_at BEFORE UPDATE ON oauth_tokens
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_baselines_updated_at BEFORE UPDATE ON quality_baselines
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_notifications_updated_at BEFORE UPDATE ON notification_queue
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE conflicts IS 'Detected data conflicts between external sources and Uberall';
COMMENT ON TABLE resolution_decisions IS 'Resolution decisions made by the engine for each conflict';
COMMENT ON TABLE location_snapshots IS 'Historical snapshots of location data from external sources';
COMMENT ON TABLE resolution_rules IS 'Configurable rules for automated conflict resolution';
COMMENT ON TABLE audit_log IS 'Comprehensive audit trail for all system actions';
COMMENT ON TABLE oauth_tokens IS 'OAuth tokens for external API authentication';
COMMENT ON TABLE ml_models IS 'Machine learning model metadata and versioning';
COMMENT ON TABLE quality_baselines IS 'Historical quality baselines for each location';
COMMENT ON TABLE idempotency_keys IS 'Idempotency keys to prevent duplicate operations';
COMMENT ON TABLE notification_queue IS 'Queue for pending notifications (Slack, Email)';