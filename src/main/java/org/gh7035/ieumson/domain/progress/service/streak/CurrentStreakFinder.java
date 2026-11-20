package org.gh7035.ieumson.domain.progress.service.streak;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.repository.DailyLearningResultRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CurrentStreakFinder {

    private final DailyLearningResultRepository dailyLearningResultRepository;

    public int find(Member member) {
        LocalDate today = LocalDate.now(LearningPolicy.ZONE);

        Optional<DailyLearningResult> todayRow =
                dailyLearningResultRepository.findByMemberAndLearnedDate(member, today);
        if (todayRow.isPresent()) {
            DailyLearningResult row = todayRow.get();
            if (Boolean.TRUE.equals(row.getCompleted()) || row.getStreakCount() > 0) {
                return row.getStreakCount();
            }
        }

        return dailyLearningResultRepository.findByMemberAndLearnedDate(member, today.minusDays(1))
                .filter(row -> Boolean.TRUE.equals(row.getCompleted()))
                .map(DailyLearningResult::getStreakCount)
                .orElse(0);
    }
}
