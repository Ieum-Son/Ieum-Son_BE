package org.gh7035.ieumson.domain.member.presentation.dto.response;

public record ProfileResponse(
        String name,
        String email,
        String loginId,
        String profileImageUrl,
        int gold,
        int streakCount,
        int longestStreakCount,
        int monthStudyCount
) {
}
