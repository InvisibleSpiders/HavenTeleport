package com.nick.teleportlocations.config;

import com.nick.teleportlocations.category.CategoryConfig;
import java.util.List;
import java.util.Map;

public record PluginConfig(
        Map<String, CategoryConfig> categories,
        String landClaimsMissingPolicy,
        boolean treatMoneyCostsAsFreeWhenEconomyMissing,
        int warmupSeconds,
        boolean cancelOnMove,
        int safeSearchRadius,
        String firstJoinTarget,
        String loginTarget,
        String deathRespawnTarget,
        List<String> deathRespawnFallback,
        int elevatorMaxDistance,
        int elevatorCooldownSeconds,
        boolean elevatorParticlesEnabled,
        String elevatorDefaultParticle,
        int elevatorParticleIntervalTicks,
        boolean tpaEnabled,
        int tpaRequestTimeoutSeconds,
        int tpaCooldownSeconds,
        int tpaWarmupSeconds,
        boolean tpaCancelWarmupOnMove,
        int tpaMaxOutgoingRequests
) {
}
