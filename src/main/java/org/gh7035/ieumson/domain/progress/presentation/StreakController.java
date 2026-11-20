package org.gh7035.ieumson.domain.progress.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.RecoverStreakResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.StreakSummaryResponse;
import org.gh7035.ieumson.domain.progress.service.streak.GetStreakSummaryService;
import org.gh7035.ieumson.domain.progress.service.streak.RecoverStreakService;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Streak", description = "스트릭 API")
@RestController
@RequestMapping("/api/streak")
@RequiredArgsConstructor
public class StreakController {

    private final RecoverStreakService recoverStreakService;
    private final GetStreakSummaryService getStreakSummaryService;

    @Operation(summary = "주간 스트릭 조회", description = "헤더/메인에 보여줄 최근 7일 스트릭과 금조각. FUNC-000-01, FUNC-000-02")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public StreakSummaryResponse getStreak(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return getStreakSummaryService.execute(userDetails);
    }

    @Operation(summary = "스트릭 회복", description = "금조각 35개로 끊긴 스트릭을 회복한다. FUNC-000-01")
    @PostMapping("/recover")
    @ResponseStatus(HttpStatus.OK)
    public RecoverStreakResponse recoverStreak(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return recoverStreakService.execute(userDetails);
    }
}
