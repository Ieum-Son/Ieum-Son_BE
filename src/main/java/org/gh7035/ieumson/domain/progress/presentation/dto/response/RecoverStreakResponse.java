package org.gh7035.ieumson.domain.progress.presentation.dto.response;

public record RecoverStreakResponse(
        int streakCount,
        int goldBalance,
        int spentGold
) {
}
