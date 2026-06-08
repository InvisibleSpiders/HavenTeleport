package com.nick.teleportlocations.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TeleportLocationTest {
    @Test
    void normalizesNamesForCommands() {
        assertThat(LocationName.normalize("  Main Base  ")).isEqualTo("main_base");
        assertThat(LocationName.normalize("Shop-One")).isEqualTo("shop-one");
    }

    @Test
    void shopWarpIsForcedPublicListedAndFree() {
        TeleportLocation shop = TeleportLocation.create(
                UUID.randomUUID(),
                "shop",
                OwnerRef.player(UUID.randomUUID()),
                "Nick's Shop",
                position(),
                AccessMode.PRIVATE,
                VisibilityMode.HIDDEN,
                CostSpec.money(25.0),
                false,
                Instant.EPOCH
        );

        assertThat(shop.accessMode()).isEqualTo(AccessMode.PUBLIC);
        assertThat(shop.visibilityMode()).isEqualTo(VisibilityMode.LISTED);
        assertThat(shop.cost().type()).isEqualTo(CostType.FREE);
    }

    @Test
    void mainHomeFlagOnlyAppliesToHomes() {
        assertThatThrownBy(() -> TeleportLocation.create(
                UUID.randomUUID(),
                "player_warp",
                OwnerRef.player(UUID.randomUUID()),
                "base",
                position(),
                AccessMode.PUBLIC,
                VisibilityMode.LISTED,
                CostSpec.free(),
                true,
                Instant.EPOCH
        )).isInstanceOf(LocationValidationException.class)
                .hasMessage("Only homes can be marked as main homes.");
    }

    private static SavedPosition position() {
        return new SavedPosition(UUID.randomUUID(), "world", 10.5, 64.0, -3.5, 90.0f, 0.0f);
    }
}
