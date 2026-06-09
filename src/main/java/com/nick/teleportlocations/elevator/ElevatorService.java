package com.nick.teleportlocations.elevator;

import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.location.SavedPosition;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class ElevatorService {
    public static final String USE_ACTION = "teleportlocations.elevator.use";

    private final ElevatorRepository repository;
    private final LandClaimsGateway landClaims;
    private final ElevatorParticle defaultParticle;
    private final Supplier<Instant> clock;

    public ElevatorService(ElevatorRepository repository, LandClaimsGateway landClaims, Supplier<Instant> clock) {
        this(repository, landClaims, ElevatorParticle.WAX_ON, clock);
    }

    public ElevatorService(
            ElevatorRepository repository,
            LandClaimsGateway landClaims,
            ElevatorParticle defaultParticle,
            Supplier<Instant> clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.landClaims = Objects.requireNonNull(landClaims, "landClaims");
        this.defaultParticle = Objects.requireNonNull(defaultParticle, "defaultParticle");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ElevatorResult place(UUID ownerId, SavedPosition position, boolean adminBypassClaims) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(position, "position");
        if (!adminBypassClaims && !landClaims.ownsClaimAt(ownerId, position)) {
            return ElevatorResult.empty(ElevatorResult.Status.CLAIM_DENIED);
        }
        Optional<ElevatorBlock> existing = findAt(position);
        if (existing.isPresent()) {
            return ElevatorResult.of(ElevatorResult.Status.UPDATED, existing.get());
        }
        Instant now = clock.get();
        ElevatorBlock block = new ElevatorBlock(UUID.randomUUID(), ownerId, normalized(position), defaultParticle, now, now);
        repository.save(block);
        return ElevatorResult.of(ElevatorResult.Status.PLACED, block);
    }

    public ElevatorResult breakBlock(UUID playerId, SavedPosition position, boolean adminBypassClaims) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(position, "position");
        Optional<ElevatorBlock> existing = findAt(position);
        if (existing.isEmpty()) {
            return ElevatorResult.empty(ElevatorResult.Status.NOT_FOUND);
        }
        if (!adminBypassClaims && !landClaims.canBuild(playerId, position)) {
            return ElevatorResult.empty(ElevatorResult.Status.ACCESS_DENIED);
        }
        repository.delete(existing.get().id());
        return ElevatorResult.of(ElevatorResult.Status.REMOVED, existing.get());
    }

    public Optional<ElevatorBlock> findDestination(SavedPosition from, ElevatorDirection direction, int maxDistance) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(direction, "direction");
        int blockX = ElevatorBlock.block(from.x());
        int blockY = ElevatorBlock.block(from.y());
        int blockZ = ElevatorBlock.block(from.z());
        return repository.findColumn(from.worldId(), blockX, blockZ).stream()
                .filter(block -> block.blockY() != blockY)
                .filter(block -> direction == ElevatorDirection.UP ? block.blockY() > blockY : block.blockY() < blockY)
                .filter(block -> Math.abs(block.blockY() - blockY) <= maxDistance)
                .min(direction == ElevatorDirection.UP
                        ? Comparator.comparingInt(ElevatorBlock::blockY)
                        : Comparator.comparingInt(ElevatorBlock::blockY).reversed());
    }

    public boolean canUse(UUID playerId, SavedPosition position, boolean adminBypassClaims) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(position, "position");
        if (findAt(position).isEmpty()) {
            return false;
        }
        return adminBypassClaims || landClaims.canInteract(playerId, position, USE_ACTION);
    }

    public ElevatorResult setParticle(UUID playerId, SavedPosition position, ElevatorParticle particle, boolean adminBypassClaims) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(particle, "particle");
        Optional<ElevatorBlock> existing = findAt(position);
        return setParticle(playerId, existing, particle, adminBypassClaims);
    }

    public ElevatorResult setParticle(UUID playerId, UUID blockId, ElevatorParticle particle, boolean adminBypassClaims) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(particle, "particle");
        Optional<ElevatorBlock> existing = repository.findById(blockId);
        return setParticle(playerId, existing, particle, adminBypassClaims);
    }

    private ElevatorResult setParticle(UUID playerId, Optional<ElevatorBlock> existing, ElevatorParticle particle, boolean adminBypassClaims) {
        if (existing.isEmpty()) {
            return ElevatorResult.empty(ElevatorResult.Status.NOT_FOUND);
        }
        ElevatorBlock current = existing.get();
        if (!adminBypassClaims && !current.ownerId().equals(playerId)) {
            return ElevatorResult.empty(ElevatorResult.Status.ACCESS_DENIED);
        }
        ElevatorBlock updated = current.withParticle(particle, clock.get());
        repository.save(updated);
        return ElevatorResult.of(ElevatorResult.Status.UPDATED, updated);
    }

    public Optional<ElevatorBlock> findAt(SavedPosition position) {
        Objects.requireNonNull(position, "position");
        return repository.findAt(
                position.worldId(),
                ElevatorBlock.block(position.x()),
                ElevatorBlock.block(position.y()),
                ElevatorBlock.block(position.z())
        );
    }

    private static SavedPosition normalized(SavedPosition position) {
        return new SavedPosition(
                position.worldId(),
                position.worldName(),
                ElevatorBlock.block(position.x()),
                ElevatorBlock.block(position.y()),
                ElevatorBlock.block(position.z()),
                position.yaw(),
                position.pitch()
        );
    }
}
