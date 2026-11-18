CREATE TABLE webhook_endpoints (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL,
    target_url VARCHAR(1000) NOT NULL,
    signing_secret VARCHAR(255) NOT NULL,
    previous_secret VARCHAR(255),
    subscribed_events VARCHAR(1000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    max_retries INTEGER NOT NULL DEFAULT 5,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_webhook_endpoints_tenant_active
    ON webhook_endpoints (tenant_id, active);

CREATE TABLE webhook_events (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    payload VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_webhook_events_tenant_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE webhook_deliveries (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    endpoint_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_http_status INTEGER,
    last_error VARCHAR(1000),
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_deliveries_event FOREIGN KEY (event_id) REFERENCES webhook_events (id),
    CONSTRAINT fk_deliveries_endpoint FOREIGN KEY (endpoint_id) REFERENCES webhook_endpoints (id)
);

CREATE INDEX idx_webhook_deliveries_due
    ON webhook_deliveries (status, next_attempt_at);

CREATE TABLE delivery_attempts (
    id VARCHAR(64) PRIMARY KEY,
    delivery_id VARCHAR(64) NOT NULL,
    attempt_number INTEGER NOT NULL,
    http_status INTEGER,
    duration_ms BIGINT NOT NULL,
    error_message VARCHAR(1000),
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_attempts_delivery FOREIGN KEY (delivery_id) REFERENCES webhook_deliveries (id)
);

CREATE INDEX idx_delivery_attempts_delivery
    ON delivery_attempts (delivery_id, attempt_number);
