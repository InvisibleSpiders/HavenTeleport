package com.nick.teleportlocations.command;

public final class CommandMessages {
    private CommandMessages() {
    }

    public static String playerUsage() {
        return "/home [name], /sethome <name>, /homes, /warp <name>, /warps, /shops, /setshop <name>, /outpost <name>, /spawn";
    }

    public static String adminUsage() {
        return "/tl reload, /tl admin setlimit <player> <category> <amount>, /tl admin setspawn, /tl admin setserverwarp <name>";
    }
}
