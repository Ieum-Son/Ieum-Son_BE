package org.gh7035.ieumson.domain.progress.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.FavoriteWordListResponse;
import org.gh7035.ieumson.domain.progress.service.AddFavoriteWordService;
import org.gh7035.ieumson.domain.progress.service.GetFavoriteWordsService;
import org.gh7035.ieumson.domain.progress.service.RemoveFavoriteWordService;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Favorites", description = "즐겨찾기 API")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final GetFavoriteWordsService getFavoriteWordsService;
    private final AddFavoriteWordService addFavoriteWordService;
    private final RemoveFavoriteWordService removeFavoriteWordService;

    @Operation(summary = "즐겨찾기 단어 목록", description = "FUNC-007-03")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public FavoriteWordListResponse getFavorites(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return getFavoriteWordsService.execute(userDetails);
    }

    @Operation(summary = "단어 즐겨찾기 추가", description = "마스터한 단어만 즐겨찾기할 수 있다. FUNC-007-03")
    @PostMapping("/{wordId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void addFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long wordId
    ) {
        addFavoriteWordService.execute(userDetails, wordId);
    }

    @Operation(summary = "단어 즐겨찾기 해제", description = "FUNC-007-03")
    @DeleteMapping("/{wordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long wordId
    ) {
        removeFavoriteWordService.execute(userDetails, wordId);
    }
}
