package org.gh7035.ieumson.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.presentation.dto.response.ProfileResponse;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.repository.DailyLearningResultRepository;
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

    public ProfileResponse execute(CustomUserDetails userDetails) {
        Member member = currentMemberFinder.get(userDetails);
        LocalDate today = LocalDate.now(LearningPolicy.ZONE);
        int streak = dailyLearningResultRepository.findTopByMemberOrderByLearnedDateDesc(member)
                .filter(last -> last.getLearnedDate().equals(today) || last.getLearnedDate().equals(today.minusDays(1)))
                .map(DailyLearningResult::getStreakCount)
                .orElse(0);

        return new ProfileResponse(
                member.getName(),
                member.getEmail(),
                member.getLoginId(),
                member.getProfileImageUrl(),
                member.getNuggetBalance(),
                streak
        );
    }
}
