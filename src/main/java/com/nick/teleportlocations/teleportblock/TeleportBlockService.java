package com.nick.teleportlocations.teleportblock;

import com.nick.teleportlocations.claim.LandClaimsGateway;
import com.nick.teleportlocations.location.SavedPosition;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class TeleportBlockService {
    public static final String USE_ACTION = "teleportlocations.teleportblock.use";

    private final TeleportBlockRepository repository;
    private final LandClaimsGateway landClaims;
    private final Supplier<Instant> clock;

    public TeleportBlockService(TeleportBlockRepository repository, LandClaimsGateway landClaims, Supplier<Instant> clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.landClaims = Objects.requireNonNull(landClaims, "landClaims");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public TeleportBlockResult place(UUID ownerId, SavedPosition position, boolean adminBypassClaims) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(position, "position");
        if (!adminBypassClaims && !landClaims.ownsClaimAt(ownerId, position)) {
            return TeleportBlockResult.empty(TeleportBlockResult.Status.CLAIM_DENIED);
        }
        Optional<TeleportBlock> existing = findAt(position);
        if (existing.isPresent()) {
            return TeleportBlockResult.of(TeleportBlockResult.Status.UPDATED, existing.get());
        }
        Instant now = clock.get();
        TeleportBlock block = new TeleportBlock(UUID.randomUUID(), ownerId, normalized(position), Optional.empty(), now, now);
        repository.save(block);
        return TeleportBlockResult.of(TeleportBlockResult.Status.PLACED, block);
    }

    public TeleportBlockResult breakBlock(UUID playerId, SavedPosition position, boolean adminBypassClaims) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(position, "position");
        Optional<TeleportBlock> existing = findAt(position);
        if (existing.isEmpty()) {
            return TeleportBlockResult.empty(TeleportBlockResult.Status.NOT_FOUND);
        }
        if (!adminBypassClaims && !landClaims.canBuild(playerId, position)) {
            return TeleportBlockResult.empty(TeleportBlockResult.Status.ACCESS_DENIED);
        }
        unlink(existing.get());
        repository.delete(existing.get().id());
        return TeleportBlockResult.of(TeleportBlockResult.Status.REMOVED, existing.get());
    }

    public TeleportBlockResult link(UUID playerId, SavedPosition firstPosition, SavedPosition secondPosition, boolean adminBypassClaims, int maxDistance) {
        Objects.requireNonNull(playerId, "playerId");
        TeleportBlock first = findAt(firstPosition).orElse(null);
        TeleportBlock second = findAt(secondPosition).orElse(null);
        if (first == null || second == null) {
            return TeleportBlockResult.empty(TeleportBlockResult.Status.NOT_FOUND);
        }
        if (first.id().equals(second.id())) {
            return TeleportBlockResult.empty(TeleportBlockResult.Status.SAME_BLOCK);
        }
        if (!canEdit(playerId, first, adminBypassClaims) || !canEdit(playerId, second, adminBypassClaims)) {
            return TeleportBlockResult.empty(TeleportBlockResult.Status.ACCESS_DENIED);
        }
        if (!withinDistance(first.position(), second.position(), maxDistance)) {
            return TeleportBlockResult.empty(TeleportBlockResult.Status.DISTANCE_TOO_FAR);
        }
        Instant now = clock.get();
        TeleportBlock linkedFirst = first.withLink(Optional.of(second.id()), now);
        TeleportBlock linkedSecond = second.withLink(Optional.of(first.id()), now);
        repository.save(linkedFirst);
        repository.save(linkedSecond);
        return TeleportBlockResult.of(TeleportBlockResult.Status.LINKED, linkedFirst);
    }

    public boolean canUse(UUID playerId, SavedPosition position, boolean adminBypassClaims) {
        return adminBypassClaims || landClaims.canInteract(playerId, position, USE_ACTION);
    }

    public Optional<TeleportBlock> linkedDestination(TeleportBlock block) {
        return repository.findById(block.id())
                .or(() -> Optional.of(block))
                .flatMap(current -> current.linkedBlockId().flatMap(repository::findById));
    }

    public Optional<TeleportBlock> findAt(SavedPosition position) {
        Objects.requireNonNull(position, "position");
        return repository.findAt(
                position.worldId(),
                TeleportBlock.block(position.x()),
                TeleportBlock.block(position.y()),
                TeleportBlock.block(position.z())
        );
    }

    private boolean canEdit(UUID playerId, TeleportBlock block, boolean adminBypassClaims) {
        return adminBypassClaims || block.ownerId().equals(playerId) || landClaims.canBuild(playerId, block.position());
    }

    private void unlink(TeleportBlock block) {
        block.linkedBlockId()
                .flatMap(repository::findById)
                .ifPresent(linked -> repository.save(linked.withLink(Optional.empty(), clock.get())));
    }

    private static boolean withinDistance(SavedPosition first, SavedPosition second, int maxDistance) {
        if (!first.worldId().equals(second.worldId())) {
            return false;
        }
        double dx = first.x() - second.x();
        double dy = first.y() - second.y();
        double dz = first.z() - second.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz) <= Math.max(0, maxDistance);
    }

    private static SavedPosition normalized(SavedPosition position) {
        return new SavedPosition(
                position.worldId(),
                position.worldName(),
                TeleportBlock.block(position.x()),
                TeleportBlock.block(position.y()),
                TeleportBlock.block(position.z()),
                position.yaw(),
                position.pitch()
        );
    }
}
