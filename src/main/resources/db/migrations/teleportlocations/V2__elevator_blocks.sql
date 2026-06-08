CREATE TABLE IF NOT EXISTS teleport_elevator_blocks (
    id VARCHAR(36) PRIMARY KEY,
    owner_uuid VARCHAR(36) NOT NULL,
    world_id VARCHAR(36) NOT NULL,
    world_name VARCHAR(128) NOT NULL,
    block_x INTEGER NOT NULL,
    block_y INTEGER NOT NULL,
    block_z INTEGER NOT NULL,
    particle VARCHAR(64) NOT NULL,
    created_at VARCHAR(64) NOT NULL,
    updated_at VARCHAR(64) NOT NULL,
    CONSTRAINT teleport_elevator_blocks_position UNIQUE(world_id, block_x, block_y, block_z)
);

CREATE INDEX IF NOT EXISTS teleport_elevator_blocks_column
    ON teleport_elevator_blocks(world_id, block_x, block_z);
