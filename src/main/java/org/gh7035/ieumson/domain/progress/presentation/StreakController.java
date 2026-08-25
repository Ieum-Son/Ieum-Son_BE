package org.gh7035.ieumson.domain.progress.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.CalendarStreakResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.GoldHistoryResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.RecoverStreakResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.StreakSummaryResponse;
import org.gh7035.ieumson.domain.progress.service.GetCalendarStreakService;
import org.gh7035.ieumson.domain.progress.service.GetGoldHistoryService;
import org.gh7035.ieumson.domain.progress.service.GetStreakSummaryService;
import org.gh7035.ieumson.domain.progress.service.RecoverStreakService;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Streak", description = "스트릭·달력·금조각 API")
@RestController
@RequestMapping("/api/streak")
@RequiredArgsConstructor
public class StreakController {

    private final RecoverStreakService recoverStreakService;
    private final GetStreakSummaryService getStreakSummaryService;
    private final GetCalendarStreakService getCalendarStreakService;
    private final GetGoldHistoryService getGoldHistoryService;

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

    @Operation(summary = "달력 스트릭 조회", description = "깃허브 잔디처럼 월별 학습 강도를 반환한다. FUNC-002-02, FUNC-007-08")
    @GetMapping("/calendar")
    @ResponseStatus(HttpStatus.OK)
    public CalendarStreakResponse getCalendar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return getCalendarStreakService.execute(userDetails, year, month);
    }

    @Operation(summary = "금조각 내역 조회", description = "획득/사용 원장을 최신순으로 조회한다. FUNC-007-10")
    @GetMapping("/gold")
    @ResponseStatus(HttpStatus.OK)
    public GoldHistoryResponse getGoldHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return getGoldHistoryService.execute(userDetails, pageable);
    }
}
