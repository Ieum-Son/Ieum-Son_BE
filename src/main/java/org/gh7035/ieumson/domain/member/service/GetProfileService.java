package org.gh7035.ieumson.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.presentation.dto.response.ProfileResponse;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.repository.DailyLearningResultRepository;
import org.gh7035.ieumson.domain.progress.service.streak.CurrentStreakFinder;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetProfileService {

    private final CurrentMemberFinder currentMemberFinder;
    private final DailyLearningResultRepository dailyLearningResultRepository;
    private final CurrentStreakFinder currentStreakFinder;

    public ProfileResponse execute(CustomUserDetails userDetails) {
        Member member = currentMemberFinder.get(userDetails);
        LocalDate today = LocalDate.now(LearningPolicy.ZONE);

        int streak = currentStreakFinder.find(member);

        int longestStreakCount = dailyLearningResultRepository
                .findLongestStreakByMember(member)
                .orElse(0);

        int monthStudyCount = dailyLearningResultRepository
                .countCompletedByMemberBetween(member, today.withDayOfMonth(1), today);

        return new ProfileResponse(
                member.getName(),
                member.getEmail(),
                member.getLoginId(),
                member.getProfileImageUrl(),
                member.getNuggetBalance(),
                streak,
                longestStreakCount,
                monthStudyCount
        );
    }
}
