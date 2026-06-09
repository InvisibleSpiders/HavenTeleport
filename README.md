# TeleportLocations

TeleportLocations is a Paper plugin for homes, server warps, player warps, free public shop warps, outposts, spawn control, and elevator block foundations.

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
| `/tpa <player>` | `teleportlocations.tpa` | Ask to teleport to another online player. |
| `/tpahere <player>` | `teleportlocations.tpahere` | Ask another online player to teleport to you. |
| `/tpaccept [player]` | `teleportlocations.tpaccept` | Accept the latest pending teleport request, or a named request. Alias: `/tpyes`. |
| `/tpdecline [player]` | `teleportlocations.tpdecline` | Decline the latest pending teleport request, or a named request. Alias: `/tpno`. |

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
| `/ht admin bypass claims [on\|off\|status]` | `teleportlocations.admin.bypass.claims` | Toggle or inspect your personal claim-bypass mode for protected elevator actions. |
| `/ht admin tp <player> <target>` | `teleportlocations.admin.teleport` | Instantly teleport one online player to another online player without a request, warmup, or cooldown. |

`/ht` aliases: `/haventeleport`, `/tl`.

Elevator placement, breaking, jump/sneak movement, recipe registration, cooldowns, ambient particles, and the particle settings dialog are active.

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
| `teleportlocations.tpa` | true | Allows sending `/tpa` requests. |
| `teleportlocations.tpahere` | true | Allows sending `/tpahere` requests. |
| `teleportlocations.tpaccept` | true | Allows accepting teleport requests. |
| `teleportlocations.tpdecline` | true | Allows declining teleport requests. |
| `teleportlocations.elevator` | true | Parent permission for elevator placement, breaking, use, menu, and default particles. |
| `teleportlocations.elevator.place` | true | Allows placing elevator blocks in owned claims. |
| `teleportlocations.elevator.break` | true | Allows breaking elevator blocks where the player has claim build access. |
| `teleportlocations.elevator.use` | true | Allows using elevator blocks where the player has claim access. |
| `teleportlocations.elevator.menu` | true | Allows opening the elevator settings dialog once wired. |
| `teleportlocations.elevator.particle.wax_on` | true | Allows selecting the default Wax On elevator particle. |
| `teleportlocations.elevator.particle.end_rod` | op | Allows selecting the End Rod elevator particle. |
| `teleportlocations.admin` | op | Parent permission for all admin permissions. |
| `teleportlocations.admin.reload` | op via `teleportlocations.admin` | Reserved for reload/admin maintenance. |
| `teleportlocations.admin.limits` | op via `teleportlocations.admin` | Allows editing player limits. |
| `teleportlocations.admin.serverwarp` | op via `teleportlocations.admin` | Allows managing server warps. |
| `teleportlocations.admin.spawn` | op via `teleportlocations.admin` | Allows setting managed spawn. |
| `teleportlocations.admin.edit` | op via `teleportlocations.admin` | Reserved for admin location editing. |
| `teleportlocations.admin.teleport` | op via `teleportlocations.admin` | Reserved for admin teleport tools. |
| `teleportlocations.admin.elevator` | op via `teleportlocations.admin` | Reserved for admin elevator management. |
| `teleportlocations.admin.bypass.creation` | op via `teleportlocations.admin` | Bypass claim/location creation checks. |
| `teleportlocations.admin.bypass.claims` | op via `teleportlocations.admin` | Allows toggling personal claim-bypass mode for protected elevator actions. |
| `teleportlocations.admin.bypass.cost` | op via `teleportlocations.admin` | Bypass teleport costs. |
| `teleportlocations.admin.bypass.cooldown` | op via `teleportlocations.admin` | Reserved for cooldown bypass. |
| `teleportlocations.admin.bypass.tpa.cooldown` | op via `teleportlocations.admin` | Bypass TPA request cooldowns. |

## Elevator Blocks

Elevator blocks are stored separately from homes, warps, shops, and outposts. They are crafted as a marked Lodestone item with this recipe:

```text
 E 
CLC
 E 
```

`E` is Echo Shard, `C` is Copper Ingot, and `L` is Lodestone. When placed, the block remains visually Lodestone, but right-click interaction is cancelled so it does not behave like a normal Lodestone.

Placement is restricted to a player's own LandClaims claim unless admin claim-bypass mode is active. Breaking follows claim build access, so trusted builders can remove elevators in claims where they can build. Players with claim access can use elevators. Jumping on an elevator moves to the nearest elevator above in the same X/Z column; sneaking moves to the nearest elevator below.

Shift-right-click an elevator block to open its settings dialog. Owners can change the particle. Admins need both `teleportlocations.admin.elevator` and active claim-bypass mode to edit someone else's elevator. The dialog only shows particle choices the player has permission to use, such as `teleportlocations.elevator.particle.wax_on` and `teleportlocations.elevator.particle.end_rod`.

Admins can run `/ht admin bypass claims` to toggle claim-bypass mode, or use `/ht admin bypass claims on`, `/ht admin bypass claims off`, and `/ht admin bypass claims status`. When active, protected elevator actions send a chat reminder so admins know they are bypassing claim checks.

Elevator defaults are configured under `elevators` in `config.yml`:

| Setting | Default | Description |
| --- | --- | --- |
| `elevators.max-distance` | `16` | Maximum vertical distance between detected elevator floors. |
| `elevators.cooldown-seconds` | `2` | Cooldown used by the upcoming in-game elevator listener. |
| `elevators.particles.enabled` | `true` | Enables the subtle elevator visual cue globally. |
| `elevators.particles.default` | `WAX_ON` | Default particle for newly placed elevator blocks. |
| `elevators.particles.interval-ticks` | `20` | How often elevator cue particles are emitted. |

## Shop Warps

Shop warps are always public, listed, and free. They cannot be configured with a cost or locked access.

## Server Warps

Admins manage global server warps with `/ht admin serverwarp set <name>`, `/ht admin serverwarp delete <name>`, and `/ht admin serverwarp list`. Players can use server warps through `/warp <name>` and the `/warps` dialog.

## Player Teleport Requests

Players can use `/tpa <player>` to request teleporting to another online player, or `/tpahere <player>` to request that another online player teleport to them. The receiving player gets a clickable chat message with `[Accept]` and `[Decline]` actions, and can also use `/tpaccept [player]` or `/tpdecline [player]`.

TPA requests are stored in memory and expire automatically. Cooldown and warmup are disabled by default. When warmup is enabled, movement can cancel the pending teleport.

Admins can use `/ht admin tp <player> <target>` to move one online player to another immediately. Admin direct teleports do not use TPA requests, cooldowns, or warmups.

## Teleport Effects

Managed teleports can play configurable particles and sounds. Departure effects play before movement, arrival effects play after a successful async teleport, and denied effects play when HavenTeleport blocks or fails a teleport.

This applies to homes, player warps, shop warps, outposts, server warps, spawn teleports, accepted TPA requests, admin direct teleports, and elevator movement. Death respawn sets the Bukkit respawn location directly, but invalid configured respawn targets still use the denied effect.

Sound audiences can be `SELF` for only the teleporting player or `NEARBY` for players within the configured radius.

| Setting | Default | Description |
| --- | --- | --- |
| `teleport.effects.enabled` | `true` | Master switch for managed teleport particles and sounds. |
| `teleport.effects.departure.enabled` | `true` | Enables effects before a teleport starts. |
| `teleport.effects.departure.particle` | `PORTAL` | Particle name for the source location. |
| `teleport.effects.departure.sound.enabled` | `true` | Enables the departure sound. |
| `teleport.effects.arrival.enabled` | `true` | Enables effects after a successful teleport. |
| `teleport.effects.arrival.particle` | `REVERSE_PORTAL` | Particle name for the destination location. |
| `teleport.effects.arrival.sound.audience` | `NEARBY` | Who hears the arrival sound. |
| `teleport.effects.denied.enabled` | `true` | Enables the denied/failed teleport effect. |
| `teleport.effects.denied.particle` | `DUST` | Particle name for denied or failed teleport attempts. |
| `teleport.effects.denied.color` | `RED` | Color used by the default dust particle. |
| `teleport.effects.denied.sound.audience` | `SELF` | Who hears denied or failed teleport sounds. |
| `*.count`, `*.radius`, `*.y-offset` | varies | Particle amount and spread controls for each phase. |
| `*.sound.name`, `*.sound.volume`, `*.sound.pitch`, `*.sound.radius` | varies | Sound controls for each phase. |

TPA defaults are configured under `tpa` in `config.yml`:

| Setting | Default | Description |
| --- | --- | --- |
| `tpa.enabled` | `true` | Enables `/tpa`, `/tpahere`, `/tpaccept`, and `/tpdecline`. |
| `tpa.request-timeout-seconds` | `60` | How long a pending request can be accepted or declined. |
| `tpa.cooldown-seconds` | `0` | Cooldown after an accepted request before the requester can send another request. |
| `tpa.warmup-seconds` | `0` | Delay after acceptance before teleporting. |
| `tpa.cancel-warmup-on-move` | `true` | Cancels non-zero warmups when the teleporting player moves blocks. |

## Teleport Costs

Player warp costs are enforced before teleporting through `/warp` or dialog actions. Owners and admins with `teleportlocations.admin.bypass.cost` do not pay their own configured costs. Shop warps remain free.

## Claim Entry Checks

TeleportLocations checks LandClaims entry access before moving a player into a claimed destination. Claimed destinations use the LandClaims action key `teleportlocations.enter`. If the player cannot enter the destination claim, the teleport is cancelled before charging costs or moving the player.

This applies to homes, player warps, shop warps, outposts, spawn, dialog teleport actions, accepted TPA requests, and elevator destinations. Admins with active claim-bypass mode can bypass the entry check.

Player warp and shop dialogs mark inaccessible destinations by default and omit their teleport action. Server owners can hide inaccessible player/shop destinations instead:

| Setting | Default | Description |
| --- | --- | --- |
| `teleport.inaccessible-destinations.mode` | `mark` | Use `mark` to show inaccessible player/shop warps with `No claim access`, or `hide` to hide them unless the viewer owns them. |
