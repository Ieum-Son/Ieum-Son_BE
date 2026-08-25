package org.gh7035.ieumson.domain.progress.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.repository.DailyLearningResultRepository;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.StreakSummaryResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.StreakSummaryResponse.StreakDayResponse;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetStreakSummaryService {

    private final CurrentMemberFinder currentMemberFinder;
    private final DailyLearningResultRepository dailyLearningResultRepository;

    public StreakSummaryResponse execute(CustomUserDetails userDetails) {
        Member member = currentMemberFinder.get(userDetails);
        LocalDate today = LocalDate.now(LearningPolicy.ZONE);
        LocalDate from = today.minusDays(6);

        Map<LocalDate, DailyLearningResult> byDate = dailyLearningResultRepository
                .findByMemberAndLearnedDateBetween(member, from, today)
                .stream()
                .collect(Collectors.toMap(DailyLearningResult::getLearnedDate, Function.identity()));

        List<StreakDayResponse> week = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(today); date = date.plusDays(1)) {
            DailyLearningResult day = byDate.get(date);
            boolean learned = day != null && (day.getWordsCompleted() > 0 || Boolean.TRUE.equals(day.getCompleted()));
            int intensity = day == null ? 0 : LearningPolicy.calendarIntensity(day.getWordsCompleted(), day.getSentencesCompleted());
            week.add(new StreakDayResponse(date, learned, intensity));
        }

        DailyLearningResult last = dailyLearningResultRepository.findTopByMemberOrderByLearnedDateDesc(member)
                .orElse(null);
        int currentStreak = resolveCurrentStreak(last, today);
        boolean recoverable = last != null && last.getLearnedDate().isBefore(today.minusDays(1));

        return new StreakSummaryResponse(
                currentStreak,
                member.getNuggetBalance(),
                recoverable,
                LearningPolicy.STREAK_RECOVERY_COST,
                week
        );
    }

    private int resolveCurrentStreak(DailyLearningResult last, LocalDate today) {
        if (last == null) {
            return 0;
        }
        if (last.getLearnedDate().equals(today) || last.getLearnedDate().equals(today.minusDays(1))) {
            return last.getStreakCount();
        }
        return 0;
    }
}
