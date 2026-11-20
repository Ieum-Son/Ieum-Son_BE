package org.gh7035.ieumson.domain.study.presentation.dto.response;

public record CompleteLearningResponse(
        int streakCount,
        int completedDayCount,
        int goldEarned,
        int goldFromWords,
        int streakBonus,
        int goldBalance,
        int wordsCompleted,
        int sentencesCompleted
) {
}
