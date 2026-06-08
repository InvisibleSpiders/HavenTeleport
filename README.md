# TeleportLocations

TeleportLocations is a Paper plugin for homes, server warps, player warps, free public shop warps, outposts, and spawn control.

## Build

```powershell
.\gradlew.bat build
```

The plugin jar is written to `build/libs/TeleportLocations-1.0.0-SNAPSHOT.jar`.

## Runtime

- Paper 26.1+.
- Java 25.
- Required: HavenCore.
- Optional: LandClaims.
- Optional through HavenCore: VaultUnlocked for money-cost player warps.
- TeleportLocations stores data through HavenCore's shared datasource and registers its own migrations at startup.

## Commands

### Player Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/home [name]` | `teleportlocations.home` | Teleport to your main home, or to a named home. |
| `/homes` | `teleportlocations.menu` | Open the homes dialog. |
| `/sethome <name>` | `teleportlocations.home` | Create or update a home at your current location. |
| `/delhome <name>` | `teleportlocations.home` | Delete one of your homes. |
| `/mainhome <name>` | `teleportlocations.home` | Set which home is used by `/home` with no name. |
| `/warp <name>` | `teleportlocations.warp` | Teleport to a visible server warp or player warp. |
| `/warps` | `teleportlocations.menu` | Open the combined server/player warps dialog. |
| `/setwarp <name>` | `teleportlocations.warp` | Create or update a player warp at your current location. |
| `/delwarp <name>` | `teleportlocations.warp` | Delete one of your player warps. |
| `/shops` | `teleportlocations.shop` | Open the shop warps dialog. Alias: `/shopwarps`. |
| `/setshop <name>` | `teleportlocations.shop` | Create or update a free public shop warp. |
| `/delshop <name>` | `teleportlocations.shop` | Delete one of your shop warps. |
| `/setoutpost <name>` | `teleportlocations.outpost` | Create or update an outpost. |
| `/outpost <name>` | `teleportlocations.outpost` | Teleport to one of your outposts. |
| `/deloutpost <name>` | `teleportlocations.outpost` | Delete one of your outposts. |
| `/spawn` | `teleportlocations.spawn` | Teleport to configured server spawn. |

### Admin Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/ht admin limits get <player> <category>` | `teleportlocations.admin.limits` | Show a player's resolved limit for a category. |
| `/ht admin limits set <player> <category> <amount>` | `teleportlocations.admin.limits` | Set a player's explicit limit override. |
| `/ht admin limits add <player> <category> <amount>` | `teleportlocations.admin.limits` | Increase a player's explicit limit override. |
| `/ht admin limits remove <player> <category> <amount>` | `teleportlocations.admin.limits` | Decrease a player's explicit limit override. |
| `/ht admin setspawn` | `teleportlocations.admin.spawn` | Set the managed server spawn at your current location. |
| `/ht setspawn` | `teleportlocations.admin.spawn` | Shortcut for setting the managed server spawn. |
| `/ht admin serverwarp set <name>` | `teleportlocations.admin.serverwarp` | Create or update a global server warp at your current location. |
| `/ht admin serverwarp delete <name>` | `teleportlocations.admin.serverwarp` | Delete a global server warp. |
| `/ht admin serverwarp list` | `teleportlocations.admin.serverwarp` | List global server warps. |

`/ht` aliases: `/haventeleport`, `/tl`.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `teleportlocations.use` | true | Allows basic TeleportLocations usage. |
| `teleportlocations.menu` | true | Allows opening location dialogs. |
| `teleportlocations.home` | true | Allows home commands. |
| `teleportlocations.warp` | true | Allows warp commands. |
| `teleportlocations.shop` | true | Allows shop warp commands. |
| `teleportlocations.outpost` | true | Allows outpost commands. |
| `teleportlocations.spawn` | true | Allows `/spawn`. |
| `teleportlocations.admin` | op | Parent permission for all admin permissions. |
| `teleportlocations.admin.reload` | op via `teleportlocations.admin` | Reserved for reload/admin maintenance. |
| `teleportlocations.admin.limits` | op via `teleportlocations.admin` | Allows editing player limits. |
| `teleportlocations.admin.serverwarp` | op via `teleportlocations.admin` | Allows managing server warps. |
| `teleportlocations.admin.spawn` | op via `teleportlocations.admin` | Allows setting managed spawn. |
| `teleportlocations.admin.edit` | op via `teleportlocations.admin` | Reserved for admin location editing. |
| `teleportlocations.admin.teleport` | op via `teleportlocations.admin` | Reserved for admin teleport tools. |
| `teleportlocations.admin.bypass.creation` | op via `teleportlocations.admin` | Bypass claim/location creation checks. |
| `teleportlocations.admin.bypass.cost` | op via `teleportlocations.admin` | Bypass teleport costs. |
| `teleportlocations.admin.bypass.cooldown` | op via `teleportlocations.admin` | Reserved for cooldown bypass. |

## Shop Warps

Shop warps are always public, listed, and free. They cannot be configured with a cost or locked access.

## Server Warps

Admins manage global server warps with `/ht admin serverwarp set <name>`, `/ht admin serverwarp delete <name>`, and `/ht admin serverwarp list`. Players can use server warps through `/warp <name>` and the `/warps` dialog.

## Teleport Costs

Player warp costs are enforced before teleporting through `/warp` or dialog actions. Owners and admins with `teleportlocations.admin.bypass.cost` do not pay their own configured costs. Shop warps remain free.
