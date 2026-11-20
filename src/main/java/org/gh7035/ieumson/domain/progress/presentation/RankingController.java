package org.gh7035.ieumson.domain.progress.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.RankingResponse;
import org.gh7035.ieumson.domain.progress.service.ranking.GetRankingService;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ranking", description = "금조각 랭킹 API")
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController{

    private final GetRankingService getRankingService;

    @Operation(summary = "랭킹 조회", description = "금조각 개수 순 랭킹. 순위, 프로필, 이름, 금조각을 반환한다. FUNC-004-01")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public RankingResponse getRanking(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return getRankingService.execute(userDetails, pageable);
    }
}
