package org.gh7035.ieumson.domain.study.presentation.dto.response;

public record WordAttemptResponse(
        Long wordId,
        boolean passed
) {
}
