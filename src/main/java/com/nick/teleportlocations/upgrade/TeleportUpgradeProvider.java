package com.nick.teleportlocations.upgrade;

import dev.invisiblespiders.haven.api.service.HavenEconomyService;
import dev.invisiblespiders.haven.api.upgrade.UpgradeCategory;
import dev.invisiblespiders.haven.api.upgrade.UpgradeDefinition;
import dev.invisiblespiders.haven.api.upgrade.UpgradeEffect;
import dev.invisiblespiders.haven.api.upgrade.UpgradeLevel;
import dev.invisiblespiders.haven.api.upgrade.UpgradeProvider;
import dev.invisiblespiders.haven.api.upgrade.UpgradeRequirement;
import dev.invisiblespiders.haven.api.upgrade.UpgradeScope;
import dev.invisiblespiders.haven.api.upgrade.UpgradeVisibility;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;

public final class TeleportUpgradeProvider implements UpgradeProvider {

    private static final String PROVIDER_ID = "haven-teleport";
    private static final String PROVIDER_DISPLAY_NAME = "Teleport";

    private static final UpgradeCategory PERSONAL_CATEGORY =
            new UpgradeCategory("personal", "Personal", "⭐", 1);

    private final List<UpgradeDefinition> definitions;

    public TeleportUpgradeProvider(ConfigurationSection upgradesSection, HavenEconomyService economy) {
        this.definitions = loadDefinitions(upgradesSection, economy);
    }

    @Override
    public String id() {
        return PROVIDER_ID;
    }

    @Override
    public String displayName() {
        return PROVIDER_DISPLAY_NAME;
    }

    @Override
    public List<UpgradeCategory> categories() {
        return List.of(PERSONAL_CATEGORY);
    }

    @Override
    public List<UpgradeDefinition> definitions() {
        return definitions;
    }

    @Override
    public Optional<UpgradeEffect> effect(String type, Map<String, String> values) {
        return Optional.empty();
    }

    @Override
    public Optional<UpgradeRequirement> requirement(String type, Map<String, String> values) {
        return Optional.empty();
    }

    private List<UpgradeDefinition> loadDefinitions(ConfigurationSection section, HavenEconomyService economy) {
        if (section == null) {
            return List.of();
        }
        List<UpgradeDefinition> result = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection upgradeSection = section.getConfigurationSection(id);
            if (upgradeSection == null) {
                continue;
            }
            List<String> names = upgradeSection.getStringList("names");
            List<Double> costs = upgradeSection.getDoubleList("costs");

            List<UpgradeLevel> levels = new ArrayList<>();
            for (int i = 0; i < names.size(); i++) {
                String levelName = names.get(i);
                double cost = i < costs.size() ? costs.get(i) : 0.0;

                List<UpgradeRequirement> requirements;
                if (economy != null && cost > 0) {
                    requirements = List.of(new MoneyRequirement(economy, cost));
                } else {
                    requirements = List.of();
                }

                levels.add(new UpgradeLevel(
                        i + 1,
                        levelName,
                        requirements,
                        List.of(),
                        Map.of()
                ));
            }

            result.add(new UpgradeDefinition(
                    id,
                    PROVIDER_ID,
                    PERSONAL_CATEGORY,
                    UpgradeScope.PLAYER,
                    UpgradeVisibility.VISIBLE,
                    null,
                    levels
            ));
        }
        return List.copyOf(result);
    }
}
