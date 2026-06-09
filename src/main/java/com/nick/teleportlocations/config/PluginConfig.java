package com.nick.teleportlocations.config;

import com.nick.teleportlocations.category.CategoryConfig;
import com.nick.teleportlocations.teleport.effect.TeleportEffectConfig;
import java.util.List;
import java.util.Map;

public record PluginConfig(
        Map<String, CategoryConfig> categories,
        String landClaimsMissingPolicy,
        boolean treatMoneyCostsAsFreeWhenEconomyMissing,
        int warmupSeconds,
        boolean cancelOnMove,
        int safeSearchRadius,
        String inaccessibleDestinationMode,
        TeleportEffectConfig teleportEffects,
        String firstJoinTarget,
        String loginTarget,
        String deathRespawnTarget,
        List<String> deathRespawnFallback,
        int elevatorMaxDistance,
        int elevatorCooldownSeconds,
        int teleportBlockMaxDistance,
        int teleportBlockCooldownSeconds,
        boolean elevatorParticlesEnabled,
        String elevatorDefaultParticle,
        int elevatorParticleIntervalTicks,
        boolean tpaEnabled,
        int tpaRequestTimeoutSeconds,
        int tpaCooldownSeconds,
        int tpaWarmupSeconds,
        boolean tpaCancelWarmupOnMove
) {
}
