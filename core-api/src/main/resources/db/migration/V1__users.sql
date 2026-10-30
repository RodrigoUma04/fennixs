REVOKE ALL ON flyway_schema_history FROM fennixs_app;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    issuer TEXT NOT NULL,
    subject TEXT NOT NULL,
    email TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (issuer, subject)
);

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

CREATE POLICY users_own_row ON users
    USING (
        id = nullif(current_setting('app.user_id', true), '')::uuid
        OR (issuer, subject) = (current_setting('app.login_iss', true), current_setting('app.login_sub', true))
    );
