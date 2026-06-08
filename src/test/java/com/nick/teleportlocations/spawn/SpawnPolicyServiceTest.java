package com.nick.teleportlocations.spawn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

final class SpawnPolicyServiceTest {
    @Test
    void deathRespawnUsesMainHomeThenBedThenSpawnThenVanilla() {
        SpawnPolicyService service = new SpawnPolicyService(new SpawnPolicy(
                SpawnTarget.SPAWN,
                SpawnTarget.LAST_LOCATION,
                SpawnTarget.MAIN_HOME,
                List.of(SpawnTarget.BED_SPAWN, SpawnTarget.SPAWN, SpawnTarget.VANILLA_WORLD_SPAWN)
        ));

        assertThat(service.deathCandidates()).containsExactly(
                SpawnTarget.MAIN_HOME,
                SpawnTarget.BED_SPAWN,
                SpawnTarget.SPAWN,
                SpawnTarget.VANILLA_WORLD_SPAWN
        );
    }

    @Test
    void disabledTargetHasNoCandidates() {
        SpawnPolicyService service = new SpawnPolicyService(new SpawnPolicy(
                SpawnTarget.DISABLED,
                SpawnTarget.DISABLED,
                SpawnTarget.DISABLED,
                List.of(SpawnTarget.SPAWN)
        ));

        assertThat(service.deathCandidates()).isEmpty();
    }
}
