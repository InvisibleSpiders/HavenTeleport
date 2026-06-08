package com.nick.teleportlocations.claim;

import com.nick.teleportlocations.category.CategoryConfig;
import com.nick.teleportlocations.category.CreationZone;
import com.nick.teleportlocations.location.SavedPosition;
import java.util.Map;
import java.util.UUID;

public final class CreationPolicyService {
    private final Map<String, CategoryConfig> categories;
    private final LandClaimsGateway landClaims;
    private final MissingLandClaimsPolicy missingPolicy;

    public CreationPolicyService(Map<String, CategoryConfig> categories, LandClaimsGateway landClaims, MissingLandClaimsPolicy missingPolicy) {
        this.categories = Map.copyOf(categories);
        this.landClaims = landClaims;
        this.missingPolicy = missingPolicy;
    }

    public ClaimAccess canCreate(UUID playerId, String categoryKey, SavedPosition position, boolean adminBypass) {
        if (adminBypass) {
            return ClaimAccess.allow();
        }
        CategoryConfig category = categories.get(categoryKey);
        if (!landClaims.available()) {
            return handleMissing(category.creationZone());
        }
        if (category.creationZone() == CreationZone.TRUSTED_CLAIM) {
            String actionKey = "teleportlocations.create." + categoryKey;
            return landClaims.canInteract(playerId, position, actionKey) ? ClaimAccess.allow() : ClaimAccess.deny("claim-denied");
        }
        if (category.creationZone() == CreationZone.WILDERNESS) {
            return landClaims.hasClaimAt(position) ? ClaimAccess.deny("claimed-land") : ClaimAccess.allow();
        }
        if (category.creationZone() == CreationZone.ADMIN) {
            return ClaimAccess.deny("admin-only");
        }
        return ClaimAccess.allow();
    }

    private ClaimAccess handleMissing(CreationZone zone) {
        if (missingPolicy == MissingLandClaimsPolicy.ALLOW) {
            return ClaimAccess.allow();
        }
        if (missingPolicy == MissingLandClaimsPolicy.DENY_ALL) {
            return ClaimAccess.deny("landclaims-missing");
        }
        return zone == CreationZone.WILDERNESS ? ClaimAccess.allow() : ClaimAccess.deny("landclaims-missing");
    }
}
