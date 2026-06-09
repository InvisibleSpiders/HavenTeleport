# Teleport Effects Design

## Goal

Add configurable teleport particles and sounds for successful, denied, and failed managed teleports.

## Scope

HavenTeleport will route plugin-owned teleports through a shared managed teleport service. That service plays departure effects before the teleport, arrival effects after a successful teleport, and failed effects when the asynchronous teleport fails. Denied effects are played by command and listener paths that reject a teleport before movement starts.

## Config

The feature is controlled by `teleport.effects.enabled`. Each phase has independent particle and sound toggles:

- `teleport.effects.departure`
- `teleport.effects.arrival`
- `teleport.effects.denied`

Each phase supports a particle name, count, radius, y offset, and sound profile. Denied supports red dust settings by default. Sound audiences are `SELF` or `NEARBY`.

## Behavior

Homes, player warps, shop warps, server warps, spawn teleports, TPA/TPAHERE, admin direct teleport, and elevators use the managed teleport service where they perform movement. Access, safety, cost, warmup, cooldown, and missing-target denials call the denied effect at the player location.

Invalid particles and sounds fall back to safe defaults. Arrival effects only play after `teleportAsync` completes successfully.

## Testing

Unit tests cover config parsing, disabled phases, success/failure sequencing in the managed teleport service, and denied effect dispatch.
