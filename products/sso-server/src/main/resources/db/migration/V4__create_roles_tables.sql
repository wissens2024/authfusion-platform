-- V4: RBAC tables
CREATE TABLE sso_roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(512),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE sso_user_roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES sso_users(id) ON DELETE CASCADE,
    role_id     UUID NOT NULL REFERENCES sso_roles(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, role_id)
);

CREATE TABLE sso_client_roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID NOT NULL REFERENCES sso_clients(id) ON DELETE CASCADE,
    role_id     UUID NOT NULL REFERENCES sso_roles(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(client_id, role_id)
);

CREATE INDEX idx_sso_user_roles_user_id ON sso_user_roles(user_id);
CREATE INDEX idx_sso_user_roles_role_id ON sso_user_roles(role_id);
CREATE INDEX idx_sso_client_roles_client_id ON sso_client_roles(client_id);
CREATE INDEX idx_sso_client_roles_role_id ON sso_client_roles(role_id);

-- Insert default roles
INSERT INTO sso_roles (id, name, description) VALUES
    (gen_random_uuid(), 'ADMIN', 'System administrator'),
    (gen_random_uuid(), 'USER', 'Standard user');
