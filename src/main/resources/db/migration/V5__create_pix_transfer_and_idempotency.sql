CREATE TABLE pix_transfers (
    id                UUID          NOT NULL,
    source_account_id UUID          NOT NULL,
    target_account_id UUID          NOT NULL,
    end_to_end_id     VARCHAR(32)   NOT NULL,
    amount            NUMERIC(19,2) NOT NULL,
    description       VARCHAR(140),
    status            VARCHAR(10)   NOT NULL,
    rejection_reason  VARCHAR(255),
    idempotency_key   VARCHAR(100)  NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    completed_at      TIMESTAMPTZ,

    CONSTRAINT pk_pix_transfers PRIMARY KEY (id),
    CONSTRAINT fk_pix_transfers_source_account FOREIGN KEY (source_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_pix_transfers_target_account FOREIGN KEY (target_account_id) REFERENCES accounts (id),
    CONSTRAINT uk_pix_transfers_end_to_end_id UNIQUE (end_to_end_id),
    CONSTRAINT uk_pix_transfers_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_pix_transfers_end_to_end_id_format CHECK (end_to_end_id ~ '^E[0-9]{8}[0-9]{12}[A-Za-z0-9]{11}$'),
    CONSTRAINT ck_pix_transfers_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_pix_transfers_distinct_accounts CHECK (source_account_id <> target_account_id),
    CONSTRAINT ck_pix_transfers_status CHECK (status IN ('PENDING', 'COMPLETED', 'REJECTED', 'FAILED')),
    CONSTRAINT ck_pix_transfers_completed_at CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL)),
    CONSTRAINT ck_pix_transfers_rejection_reason CHECK ((status = 'REJECTED') = (rejection_reason IS NOT NULL))
);

CREATE INDEX idx_pix_transfers_source_account_created_at ON pix_transfers (source_account_id, created_at);

CREATE INDEX idx_pix_transfers_target_account_created_at ON pix_transfers (target_account_id, created_at);

ALTER TABLE ledger_entries
    ADD COLUMN pix_transfer_id UUID,
    ADD CONSTRAINT fk_ledger_entries_pix_transfer FOREIGN KEY (pix_transfer_id) REFERENCES pix_transfers (id),
    ADD CONSTRAINT ck_ledger_entries_pix_transfer CHECK (
        (category IN ('PIX_IN', 'PIX_OUT')) = (pix_transfer_id IS NOT NULL)
    );

CREATE INDEX idx_ledger_entries_pix_transfer_id ON ledger_entries (pix_transfer_id);

CREATE TABLE idempotency_records (
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    response_status SMALLINT     NOT NULL,
    response_body   JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_idempotency_records PRIMARY KEY (idempotency_key),
    CONSTRAINT ck_idempotency_records_request_hash CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_idempotency_records_response_status CHECK (response_status BETWEEN 200 AND 299),
    CONSTRAINT ck_idempotency_records_expires_at CHECK (expires_at > created_at)
);

CREATE INDEX idx_idempotency_records_expires_at ON idempotency_records (expires_at);
