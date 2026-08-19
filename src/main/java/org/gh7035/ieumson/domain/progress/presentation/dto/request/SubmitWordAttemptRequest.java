package org.gh7035.ieumson.domain.progress.presentation.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record SubmitWordAttemptRequest(
        @NotNull(message = "정확도를 입력해주세요.")
        @DecimalMin(value = "0.0", message = "정확도는 0 이상이어야 합니다.")
        @DecimalMax(value = "100.0", message = "정확도는 100 이하여야 합니다.")
        Float accuracy,

        String attemptId,

        Integer elapsedSeconds
) {
}
