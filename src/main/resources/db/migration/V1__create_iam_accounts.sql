CREATE TABLE iam_accounts (
    id VARCHAR(36) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    name VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL,
    phone VARCHAR(30),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_iam_accounts_email UNIQUE (email),
    CONSTRAINT ck_iam_accounts_role CHECK (role IN ('PHYSICIAN', 'FAMILY_MEMBER'))
);
