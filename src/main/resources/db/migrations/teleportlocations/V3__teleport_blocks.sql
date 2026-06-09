CREATE TABLE IF NOT EXISTS teleport_blocks (
    id VARCHAR(36) PRIMARY KEY,
    owner_uuid VARCHAR(36) NOT NULL,
    world_id VARCHAR(36) NOT NULL,
    world_name VARCHAR(128) NOT NULL,
    block_x INTEGER NOT NULL,
    block_y INTEGER NOT NULL,
    block_z INTEGER NOT NULL,
    linked_block_id VARCHAR(36),
    created_at VARCHAR(64) NOT NULL,
    updated_at VARCHAR(64) NOT NULL,
    CONSTRAINT teleport_blocks_position UNIQUE(world_id, block_x, block_y, block_z)
);

CREATE INDEX IF NOT EXISTS teleport_blocks_link
    ON teleport_blocks(linked_block_id);
