ALTER TABLE teleport_blocks
    ADD COLUMN target_location_id VARCHAR(36);

CREATE INDEX IF NOT EXISTS teleport_blocks_target_location
    ON teleport_blocks(target_location_id);
