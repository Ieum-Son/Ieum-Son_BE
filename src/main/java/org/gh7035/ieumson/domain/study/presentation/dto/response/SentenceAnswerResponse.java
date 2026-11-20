package org.gh7035.ieumson.domain.study.presentation.dto.response;

public record SentenceAnswerResponse(
        Long sentenceId,
        boolean correct,
        Long answerWordId,
        boolean isCompleted
) {
}
