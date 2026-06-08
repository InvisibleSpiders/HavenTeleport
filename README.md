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

## Shop Warps

Shop warps are always public, listed, and free. They cannot be configured with a cost or locked access.

## Server Warps

Admins manage global server warps with `/ht admin serverwarp set <name>`, `/ht admin serverwarp delete <name>`, and `/ht admin serverwarp list`. Players can use server warps through `/warp <name>` and the `/warps` dialog.
