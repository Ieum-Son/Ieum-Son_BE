package org.gh7035.ieumson.domain.progress.presentation.dto.response;

public record CompleteLearningResponse(
        int streakCount,
        int goldEarned,
        int goldFromWords,
        int streakBonus,
        int goldBalance,
        int wordsCompleted,
        int sentencesCompleted
) {
}
