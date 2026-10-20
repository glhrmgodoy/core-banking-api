CREATE TABLE ledger_entries (
    id            UUID          NOT NULL,
    account_id    UUID          NOT NULL,
    type          VARCHAR(10)   NOT NULL,
    category      VARCHAR(20)   NOT NULL,
    amount        NUMERIC(19,2) NOT NULL,
    balance_after NUMERIC(19,2) NOT NULL,
    description   VARCHAR(140)  NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL,

    CONSTRAINT pk_ledger_entries PRIMARY KEY (id),
    CONSTRAINT fk_ledger_entries_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT ck_ledger_entries_type CHECK (type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT ck_ledger_entries_category CHECK (category IN ('DEPOSIT', 'WITHDRAWAL', 'PIX_IN', 'PIX_OUT')),
    CONSTRAINT ck_ledger_entries_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_ledger_entries_balance_after_non_negative CHECK (balance_after >= 0)
);

CREATE INDEX idx_ledger_entries_account_created_at ON ledger_entries (account_id, created_at);

CREATE FUNCTION prevent_ledger_entry_change() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entries é imutável: % não permitido', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_entries_immutable
    BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION prevent_ledger_entry_change();
