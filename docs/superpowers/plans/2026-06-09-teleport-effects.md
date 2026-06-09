# Teleport Effects Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add configurable particles and sounds for successful, denied, and failed HavenTeleport teleports.

**Architecture:** Add a small teleport effects model, a Bukkit-facing effect service, and a managed teleport service that wraps `Player#teleportAsync`. Existing command/listener paths call the managed service for actual movement and call the denied effect when they reject a teleport before movement.

**Tech Stack:** Java 21, Paper API, Bukkit scheduler, JUnit 5, Mockito.

---

### Task 1: Config Model

**Files:**
- Create: `src/main/java/com/nick/teleportlocations/teleport/effect/TeleportEffectConfig.java`
- Create: `src/main/java/com/nick/teleportlocations/teleport/effect/TeleportEffectProfile.java`
- Create: `src/main/java/com/nick/teleportlocations/teleport/effect/TeleportSoundProfile.java`
- Modify: `src/main/java/com/nick/teleportlocations/config/PluginConfig.java`
- Modify: `src/main/java/com/nick/teleportlocations/config/ConfigLoader.java`
- Test: `src/test/java/com/nick/teleportlocations/config/ConfigLoaderTest.java`

- [ ] Write failing config assertions for default departure, arrival, and denied profiles.
- [ ] Add records for effect and sound config.
- [ ] Parse `teleport.effects` from `config.yml`.
- [ ] Verify config tests pass.

### Task 2: Effect And Managed Teleport Services

**Files:**
- Create: `src/main/java/com/nick/teleportlocations/teleport/effect/TeleportEffectService.java`
- Create: `src/main/java/com/nick/teleportlocations/teleport/ManagedTeleportService.java`
- Test: `src/test/java/com/nick/teleportlocations/teleport/ManagedTeleportServiceTest.java`

- [ ] Write failing tests for departure plus arrival on success.
- [ ] Write failing tests for denied effect on failed async teleport.
- [ ] Add the managed service and effect service interfaces.
- [ ] Verify service tests pass.

### Task 3: Wire Teleport Paths

**Files:**
- Modify: `src/main/java/com/nick/teleportlocations/RuntimeServices.java`
- Modify: `src/main/java/com/nick/teleportlocations/TeleportLocationsPlugin.java`
- Modify: `src/main/java/com/nick/teleportlocations/command/AdminTeleportCommand.java`
- Modify: `src/main/java/com/nick/teleportlocations/command/PlayerLocationCommand.java`
- Modify: `src/main/java/com/nick/teleportlocations/command/TeleportRequestCommand.java`
- Modify: `src/main/java/com/nick/teleportlocations/dialog/DialogActionExecutor.java`
- Modify: `src/main/java/com/nick/teleportlocations/listener/ElevatorListener.java`
- Modify: `src/main/java/com/nick/teleportlocations/listener/SpawnListener.java`
- Update existing tests that verify direct `teleportAsync` calls.

- [ ] Inject `ManagedTeleportService` where teleports are executed.
- [ ] Replace direct `teleportAsync` calls with managed teleports.
- [ ] Call denied effects before returning from denial branches.
- [ ] Verify command/listener tests pass.

### Task 4: Docs And Verification

**Files:**
- Modify: `src/main/resources/config.yml`
- Modify: `README.md`

- [ ] Add default effect config with comments.
- [ ] Document effect config keys in the README.
- [ ] Run `.\gradlew.bat clean test build`.
- [ ] Commit and push the PR branch.
