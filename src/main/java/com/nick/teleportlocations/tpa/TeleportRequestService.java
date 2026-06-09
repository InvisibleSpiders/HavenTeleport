package com.nick.teleportlocations.tpa;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class TeleportRequestService {
    private final int requestTimeoutSeconds;
    private final int cooldownSeconds;
    private final int maxOutgoingRequests;
    private final Supplier<Instant> clock;
    private final Map<RequestKey, TeleportRequest> requests = new HashMap<>();
    private final Map<UUID, Instant> nextRequestAt = new HashMap<>();
    private final Map<UUID, Boolean> incomingEnabledByPlayer = new HashMap<>();

    public TeleportRequestService(int requestTimeoutSeconds, int cooldownSeconds, Supplier<Instant> clock) {
        this(requestTimeoutSeconds, cooldownSeconds, 3, clock);
    }

    public TeleportRequestService(int requestTimeoutSeconds, int cooldownSeconds, int maxOutgoingRequests, Supplier<Instant> clock) {
        this.requestTimeoutSeconds = Math.max(1, requestTimeoutSeconds);
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.maxOutgoingRequests = Math.max(1, maxOutgoingRequests);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public TeleportRequestResult request(UUID requesterId, UUID targetId, TeleportRequestType type, boolean bypassCooldown) {
        Objects.requireNonNull(requesterId, "requesterId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(type, "type");
        cleanupExpired();
        if (requesterId.equals(targetId)) {
            return TeleportRequestResult.selfRequest();
        }
        if (!incomingEnabled(targetId)) {
            return TeleportRequestResult.targetDisabled();
        }
        RequestKey requestKey = new RequestKey(targetId, requesterId);
        if (requests.containsKey(requestKey)) {
            return TeleportRequestResult.alreadyPending();
        }
        if (outgoingCount(requesterId) >= maxOutgoingRequests) {
            return TeleportRequestResult.outgoingLimit();
        }
        long remainingCooldown = remainingCooldownSeconds(requesterId);
        if (!bypassCooldown && remainingCooldown > 0) {
            return TeleportRequestResult.cooldown(remainingCooldown);
        }
        Instant now = clock.get();
        TeleportRequest request = new TeleportRequest(
                UUID.randomUUID(),
                requesterId,
                targetId,
                type,
                now,
                now.plusSeconds(requestTimeoutSeconds)
        );
        requests.put(requestKey, request);
        return TeleportRequestResult.requested(request);
    }

    public TeleportAcceptResult accept(UUID receiverId, Optional<UUID> requesterId) {
        Optional<TeleportRequest> request = resolvePending(receiverId, requesterId);
        if (request.isEmpty()) {
            return TeleportAcceptResult.notFound();
        }
        requests.remove(new RequestKey(receiverId, request.orElseThrow().requesterId()));
        if (cooldownSeconds > 0) {
            nextRequestAt.put(request.orElseThrow().requesterId(), clock.get().plusSeconds(cooldownSeconds));
        }
        return TeleportAcceptResult.accepted(request.orElseThrow());
    }

    public TeleportDeclineResult decline(UUID receiverId, Optional<UUID> requesterId) {
        Optional<TeleportRequest> request = resolvePending(receiverId, requesterId);
        if (request.isEmpty()) {
            return TeleportDeclineResult.notFound();
        }
        requests.remove(new RequestKey(receiverId, request.orElseThrow().requesterId()));
        return TeleportDeclineResult.declined(request.orElseThrow());
    }

    public TeleportCancelResult cancelOutgoing(UUID requesterId, Optional<UUID> targetId) {
        Optional<TeleportRequest> request = resolveOutgoing(requesterId, targetId);
        if (request.isEmpty()) {
            return TeleportCancelResult.notFound();
        }
        requests.remove(new RequestKey(request.orElseThrow().targetId(), requesterId));
        return TeleportCancelResult.cancelled(request.orElseThrow());
    }

    public Optional<TeleportRequest> pendingFor(UUID receiverId, UUID requesterId) {
        cleanupExpired();
        return Optional.ofNullable(requests.get(new RequestKey(receiverId, requesterId)));
    }

    public void clear(UUID playerId) {
        requests.entrySet().removeIf(entry -> entry.getKey().receiverId().equals(playerId)
                || entry.getKey().requesterId().equals(playerId));
        nextRequestAt.remove(playerId);
        incomingEnabledByPlayer.remove(playerId);
    }

    public boolean toggleIncoming(UUID playerId) {
        boolean enabled = !incomingEnabled(playerId);
        setIncomingEnabled(playerId, enabled);
        return enabled;
    }

    public void setIncomingEnabled(UUID playerId, boolean enabled) {
        if (enabled) {
            incomingEnabledByPlayer.remove(playerId);
        } else {
            incomingEnabledByPlayer.put(playerId, false);
        }
    }

    public boolean incomingEnabled(UUID playerId) {
        return incomingEnabledByPlayer.getOrDefault(playerId, true);
    }

    private Optional<TeleportRequest> resolvePending(UUID receiverId, Optional<UUID> requesterId) {
        cleanupExpired();
        if (requesterId.isPresent()) {
            return Optional.ofNullable(requests.get(new RequestKey(receiverId, requesterId.orElseThrow())));
        }
        return requests.values().stream()
                .filter(request -> request.targetId().equals(receiverId))
                .max(Comparator.comparing(TeleportRequest::createdAt));
    }

    private Optional<TeleportRequest> resolveOutgoing(UUID requesterId, Optional<UUID> targetId) {
        cleanupExpired();
        if (targetId.isPresent()) {
            return Optional.ofNullable(requests.get(new RequestKey(targetId.orElseThrow(), requesterId)));
        }
        return requests.values().stream()
                .filter(request -> request.requesterId().equals(requesterId))
                .max(Comparator.comparing(TeleportRequest::createdAt));
    }

    private int outgoingCount(UUID requesterId) {
        return (int) requests.values().stream()
                .filter(request -> request.requesterId().equals(requesterId))
                .count();
    }

    private long remainingCooldownSeconds(UUID requesterId) {
        if (cooldownSeconds == 0) {
            return 0;
        }
        Instant now = clock.get();
        Instant nextAllowed = nextRequestAt.get(requesterId);
        if (nextAllowed == null || !nextAllowed.isAfter(now)) {
            nextRequestAt.remove(requesterId);
            return 0;
        }
        return secondsUntil(now, nextAllowed);
    }

    private void cleanupExpired() {
        Instant now = clock.get();
        requests.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private static long secondsUntil(Instant now, Instant deadline) {
        Duration remaining = Duration.between(now, deadline);
        long seconds = remaining.getSeconds();
        if (remaining.getNano() > 0) {
            seconds++;
        }
        return Math.max(0, seconds);
    }

    private record RequestKey(UUID receiverId, UUID requesterId) {
    }
}
