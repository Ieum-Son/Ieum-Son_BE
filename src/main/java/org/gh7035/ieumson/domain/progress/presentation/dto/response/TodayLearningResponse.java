package org.gh7035.ieumson.domain.progress.presentation.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TodayLearningResponse(
        LocalDate date,
        int streakCount,
        boolean completed,
        boolean goldAwarded,
        LearningStep step,
        List<TodayWordResponse> words,
        List<TodaySentenceResponse> sentences,
        List<TodayReviewResponse> reviews,
        TodayProgressResponse progress
) {
    public enum LearningStep {
        WORD, SENTENCE, REVIEW, COMPLETE, DONE
    }

    public record TodayWordResponse(
            Long wordId,
            String word,
            String definition,
            String videoUrl,
            String category,
            int difficulty,
            float bestAccuracy,
            int attemptCount,
            boolean isMastered
    ) {
    }

    public record TodaySentenceResponse(
            Long sentenceId,
            String sentenceKor,
            boolean isCompleted,
            List<SentenceOptionResponse> options
    ) {
    }

    public record SentenceOptionResponse(
            Long wordId,
            String word,
            String videoUrl
    ) {
    }

    public record TodayReviewResponse(
            Long wordId,
            String word,
            String definition,
            String videoUrl,
            LocalDateTime lastPracticedAt,
            long elapsedHours,
            int timeLimitSeconds,
            boolean isCompleted
    ) {
    }

    public record TodayProgressResponse(
            int wordsCompleted,
            int wordsTotal,
            int sentencesCompleted,
            int sentencesTotal,
            int reviewsCompleted,
            int reviewsTotal
    ) {
    }
}
