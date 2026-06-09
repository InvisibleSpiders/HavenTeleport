package com.nick.teleportlocations.command;

public final class CommandMessages {
    private CommandMessages() {
    }

    public static String playerUsage() {
        return "/home [name], /sethome <name>, /homes, /warp <name>, /warps, /shops, /setshop <name>, /setoutpost <name>, /outpost <name>, /deloutpost <name>, /spawn, /tpa <player>, /tpahere <player>, /tpaccept [player], /tpdecline [player], /tpcancel [player], /tptoggle";
    }

    public static String adminUsage() {
        return "/ht admin limits <get|set|add|remove> <player> <category> [amount], /ht admin setspawn, /ht admin serverwarp <set|delete|list> [name], /ht admin bypass claims [on|off|status], /ht admin tp <player> <target>";
    }
}
