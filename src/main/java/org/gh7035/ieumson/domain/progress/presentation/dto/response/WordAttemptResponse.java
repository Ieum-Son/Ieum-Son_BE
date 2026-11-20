package org.gh7035.ieumson.domain.progress.presentation.dto.response;

public record WordAttemptResponse(
        Long wordId,
        float accuracy,
        float bestAccuracy,
        int attemptCount,
        boolean isMastered,
        boolean newlyMastered,
        boolean isReview,
        boolean withinTimeLimit,
        boolean canProceed
) {
}
