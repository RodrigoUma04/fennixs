CREATE TABLE refresh_tokens
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash    VARCHAR(64)  NOT NULL UNIQUE,
    user_id       UUID         NOT NULL REFERENCES users (id),
    expires_at    TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
