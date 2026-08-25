package org.gh7035.ieumson.domain.progress.presentation.dto.response;

import java.util.List;

public record FavoriteWordListResponse(
        List<FavoriteWordResponse> items
) {
    public record FavoriteWordResponse(
            Long wordId,
            String word,
            String definition,
            String videoUrl,
            String category
    ) {
    }
}
