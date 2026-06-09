package com.nick.teleportlocations.tpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class TeleportRequestServiceTest {
    @Test
    void createsTpaRequestForTargetToAccept() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        TeleportRequestService service = new TeleportRequestService(60, 0, now::get);
        UUID requester = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        TeleportRequestResult result = service.request(requester, target, TeleportRequestType.TPA, false);

        assertThat(result.status()).isEqualTo(TeleportRequestResult.Status.REQUESTED);
        assertThat(service.pendingFor(target, requester)).map(TeleportRequest::type).contains(TeleportRequestType.TPA);
    }

    @Test
    void acceptsMostRecentRequestForReceiver() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        TeleportRequestService service = new TeleportRequestService(60, 0, now::get);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        service.request(first, receiver, TeleportRequestType.TPA, false);
        now.set(Instant.EPOCH.plusSeconds(1));
        service.request(second, receiver, TeleportRequestType.TPA_HERE, false);

        TeleportAcceptResult result = service.accept(receiver, Optional.empty());

        assertThat(result.status()).isEqualTo(TeleportAcceptResult.Status.ACCEPTED);
        assertThat(result.request()).map(TeleportRequest::requesterId).contains(second);
        assertThat(result.request()).map(TeleportRequest::type).contains(TeleportRequestType.TPA_HERE);
        assertThat(service.pendingFor(receiver, second)).isEmpty();
    }

    @Test
    void declineRemovesNamedRequest() {
        TeleportRequestService service = new TeleportRequestService(60, 0, () -> Instant.EPOCH);
        UUID requester = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        service.request(requester, receiver, TeleportRequestType.TPA, false);

        TeleportDeclineResult result = service.decline(receiver, Optional.of(requester));

        assertThat(result.status()).isEqualTo(TeleportDeclineResult.Status.DECLINED);
        assertThat(service.pendingFor(receiver, requester)).isEmpty();
    }

    @Test
    void expiredRequestsCannotBeAccepted() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        TeleportRequestService service = new TeleportRequestService(5, 0, now::get);
        UUID requester = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        service.request(requester, receiver, TeleportRequestType.TPA, false);
        now.set(Instant.EPOCH.plusSeconds(6));

        TeleportAcceptResult result = service.accept(receiver, Optional.of(requester));

        assertThat(result.status()).isEqualTo(TeleportAcceptResult.Status.NOT_FOUND);
        assertThat(service.pendingFor(receiver, requester)).isEmpty();
    }

    @Test
    void cooldownStartsAfterAcceptedRequest() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.EPOCH);
        TeleportRequestService service = new TeleportRequestService(60, 10, now::get);
        UUID requester = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        service.request(requester, receiver, TeleportRequestType.TPA, false);
        service.accept(receiver, Optional.of(requester));

        TeleportRequestResult blocked = service.request(requester, receiver, TeleportRequestType.TPA, false);

        assertThat(blocked.status()).isEqualTo(TeleportRequestResult.Status.COOLDOWN);
        assertThat(blocked.remainingCooldownSeconds()).isEqualTo(10);
    }

    @Test
    void cooldownCanBeBypassed() {
        TeleportRequestService service = new TeleportRequestService(60, 10, () -> Instant.EPOCH);
        UUID requester = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        service.request(requester, receiver, TeleportRequestType.TPA, false);
        service.accept(receiver, Optional.of(requester));

        TeleportRequestResult allowed = service.request(requester, receiver, TeleportRequestType.TPA, true);

        assertThat(allowed.status()).isEqualTo(TeleportRequestResult.Status.REQUESTED);
    }
}
