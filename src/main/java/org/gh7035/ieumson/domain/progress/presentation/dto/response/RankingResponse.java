package org.gh7035.ieumson.domain.progress.presentation.dto.response;

import java.util.List;

public record RankingResponse(
        int myRank,
        int myGold,
        long totalMembers,
        List<RankingItemResponse> items
) {
    public record RankingItemResponse(
            int rank,
            String name,
            String profileImageUrl,
            int gold,
            boolean isMe
    ) {
    }
}
