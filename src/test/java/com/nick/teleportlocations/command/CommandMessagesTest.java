package com.nick.teleportlocations.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class CommandMessagesTest {
    @Test
    void playerUsageMentionsHomeAndWarpCommands() {
        assertThat(CommandMessages.playerUsage()).contains("/home", "/warps", "/shops", "/setoutpost", "/deloutpost");
    }

    @Test
    void adminUsageMentionsLimitAndSpawnCommands() {
        assertThat(CommandMessages.adminUsage()).contains("/ht admin", "limits", "setspawn", "serverwarp", "/ht admin bypass claims");
    }
}
