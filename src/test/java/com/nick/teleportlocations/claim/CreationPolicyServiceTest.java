package com.nick.teleportlocations.claim;

import static org.assertj.core.api.Assertions.assertThat;

import com.nick.teleportlocations.category.CategoryConfig;
import com.nick.teleportlocations.category.CreationZone;
import com.nick.teleportlocations.category.OwnerKind;
import com.nick.teleportlocations.location.AccessMode;
import com.nick.teleportlocations.location.SavedPosition;
import com.nick.teleportlocations.location.VisibilityMode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CreationPolicyServiceTest {
    @Test
    void claimRequiredCategoryIsDeniedWhenLandClaimsMissingByDefault() {
        CreationPolicyService service = new CreationPolicyService(categories(), LandClaimsGateway.missing(), MissingLandClaimsPolicy.DENY_CLAIM_REQUIRED);

        assertThat(service.canCreate(UUID.randomUUID(), "home", position(), false).allowed()).isFalse();
    }

    @Test
    void wildernessOutpostIsAllowedOnlyOutsideClaims() {
        CreationPolicyService outside = new CreationPolicyService(categories(), LandClaimsGateway.fixed(false, false), MissingLandClaimsPolicy.DENY_CLAIM_REQUIRED);
        CreationPolicyService inside = new CreationPolicyService(categories(), LandClaimsGateway.fixed(true, true), MissingLandClaimsPolicy.DENY_CLAIM_REQUIRED);

        assertThat(outside.canCreate(UUID.randomUUID(), "outpost", position(), false).allowed()).isTrue();
        assertThat(inside.canCreate(UUID.randomUUID(), "outpost", position(), false).allowed()).isFalse();
    }

    @Test
    void adminBypassAllowsCreation() {
        CreationPolicyService service = new CreationPolicyService(categories(), LandClaimsGateway.missing(), MissingLandClaimsPolicy.DENY_ALL);

        assertThat(service.canCreate(UUID.randomUUID(), "home", position(), true).allowed()).isTrue();
    }

    private static Map<String, CategoryConfig> categories() {
        return Map.of(
                "home", new CategoryConfig("home", OwnerKind.PLAYER, 3, CreationZone.TRUSTED_CLAIM, AccessMode.PRIVATE, VisibilityMode.HIDDEN, false, false, false),
                "outpost", new CategoryConfig("outpost", OwnerKind.PLAYER, 1, CreationZone.WILDERNESS, AccessMode.PRIVATE, VisibilityMode.HIDDEN, false, false, false)
        );
    }

    private static SavedPosition position() {
        return new SavedPosition(UUID.randomUUID(), "world", 0.0, 64.0, 0.0, 0.0f, 0.0f);
    }
}
