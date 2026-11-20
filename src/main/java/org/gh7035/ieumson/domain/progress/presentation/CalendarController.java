package org.gh7035.ieumson.domain.progress.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.CalendarStreakResponse;
import org.gh7035.ieumson.domain.progress.service.streak.GetCalendarStreakService;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Calendar", description = "학습 달력 API")
@RestController
@RequestMapping("/api/streak/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final GetCalendarStreakService getCalendarStreakService;

    @Operation(summary = "달력 스트릭 조회", description = "깃허브 잔디처럼 월별 학습 강도를 반환한다. FUNC-002-02, FUNC-007-08")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public CalendarStreakResponse getCalendar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return getCalendarStreakService.execute(userDetails, year, month);
    }
}
