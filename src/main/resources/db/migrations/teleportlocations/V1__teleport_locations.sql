CREATE TABLE IF NOT EXISTS teleport_locations (
    id VARCHAR(36) PRIMARY KEY,
    category VARCHAR(64) NOT NULL,
    owner_type VARCHAR(32) NOT NULL,
    owner_uuid VARCHAR(36),
    name VARCHAR(64) NOT NULL,
    normalized_name VARCHAR(64) NOT NULL,
    world_id VARCHAR(36) NOT NULL,
    world_name VARCHAR(128) NOT NULL,
    x DOUBLE NOT NULL,
    y DOUBLE NOT NULL,
    z DOUBLE NOT NULL,
    yaw REAL NOT NULL,
    pitch REAL NOT NULL,
    access_mode VARCHAR(32) NOT NULL,
    visibility_mode VARCHAR(32) NOT NULL,
    cost_type VARCHAR(32) NOT NULL,
    cost_amount DOUBLE NOT NULL,
    cost_item_material VARCHAR(64) NOT NULL,
    cost_item_amount INTEGER NOT NULL,
    main_home INTEGER NOT NULL,
    created_at VARCHAR(64) NOT NULL,
    updated_at VARCHAR(64) NOT NULL,
    CONSTRAINT teleport_locations_identity UNIQUE (owner_type, owner_uuid, category, normalized_name)
);

CREATE TABLE IF NOT EXISTS teleport_location_limits (
    player_uuid VARCHAR(36) NOT NULL,
    category VARCHAR(64) NOT NULL,
    limit_amount INTEGER NOT NULL,
    PRIMARY KEY(player_uuid, category)
);
