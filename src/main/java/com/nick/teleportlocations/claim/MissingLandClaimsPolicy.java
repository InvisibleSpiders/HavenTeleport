package com.nick.teleportlocations.claim;

public enum MissingLandClaimsPolicy {
    ALLOW,
    DENY_CLAIM_REQUIRED,
    DENY_ALL;

    public static MissingLandClaimsPolicy parse(String value) {
        return MissingLandClaimsPolicy.valueOf(value.trim().replace('-', '_').toUpperCase());
    }
}
