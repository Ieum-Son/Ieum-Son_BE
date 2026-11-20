package org.gh7035.ieumson.infrastructure.ticket;

public record AttemptClaims(
        long memberId,
        long sessionId,
        long wordId,
        boolean correct
) {
}
