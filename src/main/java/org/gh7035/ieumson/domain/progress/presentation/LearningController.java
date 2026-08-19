package org.gh7035.ieumson.domain.progress.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.progress.presentation.dto.request.SubmitSentenceAnswerRequest;
import org.gh7035.ieumson.domain.progress.presentation.dto.request.SubmitWordAttemptRequest;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.CompleteLearningResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.SentenceAnswerResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.TodayLearningResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.WordAttemptResponse;
import org.gh7035.ieumson.domain.progress.service.CompleteLearningService;
import org.gh7035.ieumson.domain.progress.service.GetTodayLearningService;
import org.gh7035.ieumson.domain.progress.service.SubmitSentenceAnswerService;
import org.gh7035.ieumson.domain.progress.service.SubmitWordAttemptService;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Learning", description = "오늘의 학습 API")
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class LearningController {

    private final GetTodayLearningService getTodayLearningService;
    private final SubmitWordAttemptService submitWordAttemptService;
    private final SubmitSentenceAnswerService submitSentenceAnswerService;
    private final CompleteLearningService completeLearningService;

    @Operation(summary = "오늘의 학습 세션 조회", description = "하루에 단어 5개 + 문장 빈칸 + 복습을 배정한다. FUNC-006-01/02/03")
    @GetMapping("/today")
    @ResponseStatus(HttpStatus.OK)
    public TodayLearningResponse getToday(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return getTodayLearningService.execute(userDetails);
    }

    @Operation(summary = "단어 실습 결과 제출", description = "카메라 인식 정확도를 기록한다. 90% 이상이면 다음 단어로 진행. FUNC-006-01/03")
    @PostMapping("/words/{wordId}/attempts")
    @ResponseStatus(HttpStatus.OK)
    public WordAttemptResponse submitWordAttempt(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long wordId,
            @RequestBody @Valid SubmitWordAttemptRequest request
    ) {
        return submitWordAttemptService.execute(userDetails, wordId, request);
    }

    @Operation(summary = "문장 빈칸 답안 제출", description = "수화 영상 보기 중 선택한 단어가 정답인지 채점한다. FUNC-006-02")
    @PostMapping("/sentences/{sentenceId}/answers")
    @ResponseStatus(HttpStatus.OK)
    public SentenceAnswerResponse submitSentenceAnswer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sentenceId,
            @RequestBody @Valid SubmitSentenceAnswerRequest request
    ) {
        return submitSentenceAnswerService.execute(userDetails, sentenceId, request);
    }

    @Operation(summary = "학습 마무리", description = "단어 1개당 금조각 1개, 연속 학습 시 2~3개 보너스. FUNC-006-04")
    @PostMapping("/complete")
    @ResponseStatus(HttpStatus.OK)
    public CompleteLearningResponse complete(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return completeLearningService.execute(userDetails);
    }
}
