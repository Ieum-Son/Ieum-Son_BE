package org.gh7035.ieumson.domain.progress.service.streak;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.repository.DailyLearningResultRepository;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.CalendarStreakResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.CalendarStreakResponse.CalendarDayResponse;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCalendarStreakService {

    private final CurrentMemberFinder currentMemberFinder;
    private final DailyLearningResultRepository dailyLearningResultRepository;

    public CalendarStreakResponse execute(CustomUserDetails userDetails, Integer year, Integer month) {
        Member member = currentMemberFinder.get(userDetails);

        LocalDate today = LocalDate.now(LearningPolicy.ZONE);
        YearMonth yearMonth = (year == null || month == null)
                ? YearMonth.from(today)
                : YearMonth.of(year, month);

        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();
        Map<LocalDate, DailyLearningResult> byDate = dailyLearningResultRepository
                .findByMemberAndLearnedDateBetween(member, from, to)
                .stream()
                .collect(Collectors.toMap(DailyLearningResult::getLearnedDate, Function.identity()));

        List<CalendarDayResponse> days = from.datesUntil(to.plusDays(1))
                .map(date -> {
                    DailyLearningResult day = byDate.get(date);
                    int words = day == null ? 0 : day.getWordsCompleted();
                    int sentences = day == null ? 0 : day.getSentencesCompleted();
                    return new CalendarDayResponse(
                            date,
                            LearningPolicy.calendarIntensity(words, sentences)
                    );
                })
                .toList();

        return new CalendarStreakResponse(yearMonth.getYear(), yearMonth.getMonthValue(), days);
    }
}
