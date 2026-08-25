package org.gh7035.ieumson.domain.progress.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.GoldTransaction;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.enums.GoldTransactionType;
import org.gh7035.ieumson.domain.progress.domain.repository.DailyLearningResultRepository;
import org.gh7035.ieumson.domain.progress.domain.repository.GoldTransactionRepository;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.RecoverStreakResponse;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class RecoverStreakService {

    private final CurrentMemberFinder currentMemberFinder;
    private final TodaySessionService todaySessionService;
    private final DailyLearningResultRepository dailyLearningResultRepository;
    private final GoldTransactionRepository goldTransactionRepository;

    public RecoverStreakResponse execute(CustomUserDetails userDetails) {
        Member member = currentMemberFinder.get(userDetails);
        LocalDate today = LocalDate.now(LearningPolicy.ZONE);
        DailyLearningResult previous = dailyLearningResultRepository
                .findTopByMemberAndLearnedDateBeforeOrderByLearnedDateDesc(member, today)
                .orElseThrow(() -> new IeumException(ErrorCode.STREAK_NOT_BROKEN));

        if (previous.getLearnedDate().equals(today.minusDays(1))) {
            throw new IeumException(ErrorCode.STREAK_NOT_BROKEN);
        }
        if (member.getNuggetBalance() < LearningPolicy.STREAK_RECOVERY_COST) {
            throw new IeumException(ErrorCode.INSUFFICIENT_GOLD);
        }

        member.deductGold(LearningPolicy.STREAK_RECOVERY_COST);
        goldTransactionRepository.save(GoldTransaction.of(
                member,
                GoldTransactionType.SPEND_STREAK_RECOVERY,
                LearningPolicy.STREAK_RECOVERY_COST,
                "스트릭 회복"
        ));

        DailyLearningResult todayResult = todaySessionService.getOrCreate(member);
        todayResult.restoreStreak(previous.getStreakCount() + 1);

        return new RecoverStreakResponse(
                todayResult.getStreakCount(),
                member.getNuggetBalance(),
                LearningPolicy.STREAK_RECOVERY_COST
        );
    }
}
