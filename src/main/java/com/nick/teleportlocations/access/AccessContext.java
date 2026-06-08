package com.nick.teleportlocations.access;

import java.util.UUID;

public record AccessContext(UUID viewerId, boolean admin, boolean trustedAtLocation) {
}
