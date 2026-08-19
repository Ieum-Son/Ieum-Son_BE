package org.gh7035.ieumson.domain.progress.presentation.dto.response;

public record SentenceAnswerResponse(
        Long sentenceId,
        boolean correct,
        Long answerWordId,
        boolean isCompleted
) {
}
