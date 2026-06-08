package com.nick.teleportlocations.command;

public final class CommandMessages {
    private CommandMessages() {
    }

    public static String playerUsage() {
        return "/home [name], /sethome <name>, /homes, /warp <name>, /warps, /shops, /setshop <name>, /setoutpost <name>, /outpost <name>, /deloutpost <name>, /spawn";
    }

    public static String adminUsage() {
        return "/ht admin limits <get|set|add|remove> <player> <category> [amount], /ht admin setspawn, /ht admin setserverwarp <name>";
    }
}
