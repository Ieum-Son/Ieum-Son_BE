package org.gh7035.ieumson.domain.progress.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record SubmitSentenceAnswerRequest(
        @NotNull(message = "선택한 단어를 입력해주세요.")
        Long selectedWordId
) {
}
