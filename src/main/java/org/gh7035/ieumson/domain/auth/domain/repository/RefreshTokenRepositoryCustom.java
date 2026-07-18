package org.gh7035.ieumson.domain.auth.domain.repository;

public interface RefreshTokenRepositoryCustom {

    /**
     * @return 1 if rotated, 0 if token mismatch, -1 if key not found
     */
    int rotateIfMatches(String loginId, String expectedToken, String newToken, long ttlMs);
}
