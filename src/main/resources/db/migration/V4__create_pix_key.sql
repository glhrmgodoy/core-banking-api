CREATE TABLE pix_keys (
    id         UUID         NOT NULL,
    account_id UUID         NOT NULL,
    key_type   VARCHAR(10)  NOT NULL,
    key_value  VARCHAR(100) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_pix_keys PRIMARY KEY (id),
    CONSTRAINT fk_pix_keys_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT ck_pix_keys_key_type CHECK (key_type IN ('CPF', 'EMAIL', 'PHONE', 'RANDOM')),
    CONSTRAINT ck_pix_keys_key_value_format CHECK (
        (key_type = 'CPF'    AND key_value ~ '^[0-9]{11}$') OR
        (key_type = 'EMAIL'  AND key_value ~ '^[^@\s]+@[^@\s]+$' AND key_value = LOWER(key_value)) OR
        (key_type = 'PHONE'  AND key_value ~ '^\+[1-9][0-9]{1,14}$') OR
        (key_type = 'RANDOM' AND key_value ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$')
    )
);

CREATE UNIQUE INDEX uk_pix_keys_key_value_active ON pix_keys (key_value) WHERE active;

CREATE INDEX idx_pix_keys_account_id ON pix_keys (account_id);
