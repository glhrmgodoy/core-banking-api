CREATE SEQUENCE account_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE accounts (
    id                   UUID          NOT NULL,
    customer_id          UUID          NOT NULL,
    agency               VARCHAR(4)    NOT NULL,
    number               VARCHAR(10)   NOT NULL,
    type                 VARCHAR(10)   NOT NULL,
    balance              NUMERIC(19,2) NOT NULL,
    daily_transfer_limit NUMERIC(19,2) NOT NULL,
    night_transfer_limit NUMERIC(19,2) NOT NULL,
    status               VARCHAR(10)   NOT NULL,
    version              BIGINT        NOT NULL,
    created_at           TIMESTAMPTZ   NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT uk_accounts_number UNIQUE (number),
    CONSTRAINT ck_accounts_type CHECK (type IN ('CHECKING', 'SAVINGS')),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    CONSTRAINT ck_accounts_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT ck_accounts_limits_non_negative CHECK (daily_transfer_limit >= 0 AND night_transfer_limit >= 0)
);

CREATE UNIQUE INDEX uk_accounts_customer_type_open ON accounts (customer_id, type) WHERE status <> 'CLOSED';

CREATE INDEX idx_accounts_customer_id ON accounts (customer_id);
