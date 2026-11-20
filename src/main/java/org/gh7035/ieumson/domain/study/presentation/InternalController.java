package org.gh7035.ieumson.domain.study.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.study.presentation.dto.response.WordAttemptResponse;
import org.gh7035.ieumson.domain.study.service.SubmitWordAttemptService;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.infrastructure.ticket.AttemptClaims;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Internal", description = "학습 시도 기록 API")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final SubmitWordAttemptService submitWordAttemptService;

    @Operation(summary = "단어 통과 기록", description = "AI 서버가 티켓 단어와 맞은 채점 결과를 attempt 토큰으로 기록한다.")
    @PostMapping("/words/{wordId}/attempts")
    @ResponseStatus(HttpStatus.OK)
    public WordAttemptResponse submitWordAttempt(
            @AuthenticationPrincipal AttemptClaims attempt,
            @PathVariable Long wordId
    ) {
        if (attempt == null || wordId == null || attempt.wordId() != wordId) {
            throw new IeumException(ErrorCode.INVALID_TOKEN);
        }
        return submitWordAttemptService.execute(attempt);
    }
}
