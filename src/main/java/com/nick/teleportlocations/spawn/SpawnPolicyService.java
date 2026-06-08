package com.nick.teleportlocations.spawn;

import java.util.ArrayList;
import java.util.List;

public final class SpawnPolicyService {
    private final SpawnPolicy policy;

    public SpawnPolicyService(SpawnPolicy policy) {
        this.policy = policy;
    }

    public List<SpawnTarget> firstJoinCandidates() {
        return candidates(policy.firstJoin(), List.of());
    }

    public List<SpawnTarget> loginCandidates() {
        return candidates(policy.login(), List.of());
    }

    public List<SpawnTarget> deathCandidates() {
        return candidates(policy.deathRespawn(), policy.deathFallback());
    }

    private List<SpawnTarget> candidates(SpawnTarget primary, List<SpawnTarget> fallback) {
        if (primary == SpawnTarget.DISABLED) {
            return List.of();
        }
        List<SpawnTarget> result = new ArrayList<>();
        result.add(primary);
        result.addAll(fallback);
        return List.copyOf(result);
    }
}
