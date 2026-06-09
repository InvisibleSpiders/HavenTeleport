package com.nick.teleportlocations.admin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AdminBypassService {
    private final Set<UUID> claimBypassPlayers = new HashSet<>();

    public boolean claims(UUID playerId) {
        return claimBypassPlayers.contains(playerId);
    }

    public boolean toggleClaims(UUID playerId) {
        if (claimBypassPlayers.remove(playerId)) {
            return false;
        }
        claimBypassPlayers.add(playerId);
        return true;
    }

    public void setClaims(UUID playerId, boolean enabled) {
        if (enabled) {
            claimBypassPlayers.add(playerId);
        } else {
            claimBypassPlayers.remove(playerId);
        }
    }
}
