package org.gh7035.ieumson.domain.progress.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.GoldTransaction;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.enums.GoldTransactionType;
import org.gh7035.ieumson.domain.progress.domain.repository.GoldTransactionRepository;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.CompleteLearningResponse;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompleteLearningService {

    private final CurrentMemberFinder currentMemberFinder;
    private final TodaySessionService todaySessionService;
    private final GoldTransactionRepository goldTransactionRepository;

    public CompleteLearningResponse execute(CustomUserDetails userDetails) {
        Member member = currentMemberFinder.get(userDetails);
        DailyLearningResult today = todaySessionService.getOrCreate(member);

        if (!today.isReadyToComplete()) {
            throw new IeumException(ErrorCode.LEARNING_NOT_COMPLETED);
        }

        int goldFromWords = today.getWordsCompleted() * LearningPolicy.GOLD_PER_WORD;
        int streakBonus = 0;
        if (Boolean.TRUE.equals(today.getGoldAwarded())) {
            return new CompleteLearningResponse(
                    today.getStreakCount(),
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
                goldFromWords + streakBonus,
                goldFromWords,
                streakBonus,
                member.getNuggetBalance(),
                today.getWordsCompleted(),
                today.getSentencesCompleted()
        );
    }
}
