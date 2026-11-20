package org.gh7035.ieumson.domain.progress.presentation.dto.response;

import java.time.LocalDate;
import java.util.List;

public record StreakSummaryResponse(
        int currentStreak,
        int goldBalance,
        boolean recoverable,
        int recoveryCost,
        List<StreakDayResponse> week
) {
    public record StreakDayResponse(
            LocalDate date,
            boolean learned,
            int wordsCompleted
    ) {
    }
}
