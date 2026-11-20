package org.gh7035.ieumson.domain.progress.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record FavoriteWordListResponse(
        List<FavoriteWordResponse> items
) {
    public record FavoriteWordResponse(
            Long wordId,
            String word,
            LocalDateTime recentLearnedDate, // todo recentLearnedDate (2026.07.04)
            String videoUrl,
            String category
    ) {
    }
}
