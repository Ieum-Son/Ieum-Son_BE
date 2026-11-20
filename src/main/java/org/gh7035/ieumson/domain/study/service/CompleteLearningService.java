package org.gh7035.ieumson.domain.study.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.GoldTransaction;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.enums.GoldTransactionType;
import org.gh7035.ieumson.domain.progress.domain.repository.DailyLearningResultRepository;
import org.gh7035.ieumson.domain.progress.domain.repository.GoldTransactionRepository;
import org.gh7035.ieumson.domain.study.presentation.dto.response.CompleteLearningResponse;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class CompleteLearningService {

    private final CurrentMemberFinder currentMemberFinder;
    private final TodaySessionService todaySessionService;
    private final DailyLearningResultRepository dailyLearningResultRepository;
    private final GoldTransactionRepository goldTransactionRepository;

    public CompleteLearningResponse execute(CustomUserDetails userDetails) {
        Member member = currentMemberFinder.get(userDetails);
        DailyLearningResult today = todaySessionService.findToday(member)
                .orElseThrow(() -> new IeumException(ErrorCode.TODAY_SESSION_NOT_FOUND));

        if (!today.isReadyToComplete()) {
            throw new IeumException(ErrorCode.LEARNING_NOT_COMPLETED);
        }

        today.confirmStreak(resolveConfirmedStreak(member, today));

        int goldFromWords = today.getWordsCompleted() * LearningPolicy.GOLD_PER_WORD;
        int completedDayCount = completedDayCount(member, today);
        int streakBonus = 0;
        if (dailyLearningResultRepository.claimGoldAward(today.getId()) == 0) {
            return new CompleteLearningResponse(
                    today.getStreakCount(),
                    completedDayCount,
                    0,
                    goldFromWords,
                    0,
                    member.getNuggetBalance(),
                    today.getWordsCompleted(),
                    today.getSentencesCompleted()
            );
        }

        if (goldFromWords > 0) {
            member.addGold(goldFromWords);
            goldTransactionRepository.save(GoldTransaction.of(
                    member,
                    GoldTransactionType.EARN_LEARNING,
                    goldFromWords,
                    today.getLearnedDate().getMonthValue() + "/" + today.getLearnedDate().getDayOfMonth() + " 학습 완료"
            ));
        }

        if (today.getStreakCount() >= 2) {
            streakBonus = LearningPolicy.randomStreakBonus();
            member.addGold(streakBonus);
            goldTransactionRepository.save(GoldTransaction.of(
                    member,
                    GoldTransactionType.EARN_STREAK_BONUS,
                    streakBonus,
                    "연속 학습 보너스"
            ));
        }

        today.markCompleted();
        today.markGoldAwarded();

        return new CompleteLearningResponse(
                today.getStreakCount(),
                completedDayCount,
                goldFromWords + streakBonus,
                goldFromWords,
                streakBonus,
                member.getNuggetBalance(),
                today.getWordsCompleted(),
                today.getSentencesCompleted()
        );
    }

    private int resolveConfirmedStreak(Member member, DailyLearningResult today) {
        if (today.getStreakCount() != null && today.getStreakCount() > 0) {
            return today.getStreakCount();
        }
        LocalDate yesterday = today.getLearnedDate().minusDays(1);
        return dailyLearningResultRepository.findByMemberAndLearnedDate(member, yesterday)
                .filter(row -> Boolean.TRUE.equals(row.getCompleted()))
                .map(row -> row.getStreakCount() + 1)
                .orElse(1);
    }

    private int completedDayCount(Member member, DailyLearningResult today) {
        int completedDays = dailyLearningResultRepository.countCompletedByMember(member);
        if (Boolean.TRUE.equals(today.getCompleted())) {
            return completedDays;
        }
        return completedDays + 1;
    }
}
