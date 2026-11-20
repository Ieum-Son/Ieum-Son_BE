package org.gh7035.ieumson.domain.progress.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.GoldHistoryResponse;
import org.gh7035.ieumson.domain.progress.service.ranking.GetGoldHistoryService;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Gold", description = "금조각 내역 API")
@RestController
@RequestMapping("/api/streak/gold")
@RequiredArgsConstructor
public class GoldController {

    private final GetGoldHistoryService getGoldHistoryService;

    @Operation(summary = "금조각 내역 조회", description = "획득/사용 원장을 최신순으로 조회한다. FUNC-007-10")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public GoldHistoryResponse getGoldHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return getGoldHistoryService.execute(userDetails, pageable);
    }
}
