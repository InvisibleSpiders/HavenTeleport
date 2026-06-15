package com.nick.teleportlocations.claim;

public enum MissingHavenClaimsPolicy {
    ALLOW,
    DENY_CLAIM_REQUIRED,
    DENY_ALL;

    public static MissingHavenClaimsPolicy parse(String value) {
        return MissingHavenClaimsPolicy.valueOf(value.trim().replace('-', '_').toUpperCase());
    }
}
