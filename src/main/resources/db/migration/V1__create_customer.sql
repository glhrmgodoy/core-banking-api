CREATE TABLE customers (
    id          UUID         NOT NULL,
    external_id UUID,
    full_name   VARCHAR(100) NOT NULL,
    cpf         VARCHAR(11)  NOT NULL,
    email       VARCHAR(100) NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    birth_date  DATE         NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_external_id UNIQUE (external_id),
    CONSTRAINT uk_customers_cpf UNIQUE (cpf),
    CONSTRAINT uk_customers_email UNIQUE (email),
    CONSTRAINT ck_customers_cpf_digits CHECK (cpf ~ '^[0-9]{11}$')
);
