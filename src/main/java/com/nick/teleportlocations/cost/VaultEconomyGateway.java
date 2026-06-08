package com.nick.teleportlocations.cost;

import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicesManager;

public final class VaultEconomyGateway implements EconomyGateway {
    private final Economy economy;

    private VaultEconomyGateway(Economy economy) {
        this.economy = economy;
    }

    public static EconomyGateway discover(ServicesManager servicesManager) {
        Economy economy = servicesManager.load(Economy.class);
        return economy == null ? EconomyGateway.unavailable() : new VaultEconomyGateway(economy);
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        if (!economy.has(player, amount)) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
}
