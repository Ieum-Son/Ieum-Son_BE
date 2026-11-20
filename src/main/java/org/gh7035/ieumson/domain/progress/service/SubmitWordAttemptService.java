package org.gh7035.ieumson.domain.progress.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.WordLearning;
import org.gh7035.ieumson.domain.progress.domain.repository.WordLearningRepository;
import org.gh7035.ieumson.domain.progress.presentation.dto.request.SubmitWordAttemptRequest;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.WordAttemptResponse;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.domain.study.domain.repository.WordRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmitWordAttemptService {

    private static final String ATTEMPT_KEY_PREFIX = "learning:attempt:";

    private final CurrentMemberFinder currentMemberFinder;
    private final TodaySessionService todaySessionService;
    private final WordRepository wordRepository;
    private final WordLearningRepository wordLearningRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public WordAttemptResponse execute(CustomUserDetails userDetails, Long wordId, SubmitWordAttemptRequest request) {
        Member member = currentMemberFinder.get(userDetails);
        DailyLearningResult today = todaySessionService.getOrCreate(member);
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IeumException(ErrorCode.WORD_NOT_FOUND));

        boolean isReview = today.hasAssignedReview(wordId);
        boolean isAssignedWord = today.hasAssignedWord(wordId);
        if (!isAssignedWord && !isReview) {
            throw new IeumException(ErrorCode.WORD_NOT_IN_TODAY_SESSION);
        }
        if (isReview && request.elapsedSeconds() == null) {
            throw new IeumException(ErrorCode.REVIEW_TIME_LIMIT_REQUIRED);
        }

        WordLearning learning = wordLearningRepository.findByMemberAndWord(member, word)
                .orElseGet(() -> wordLearningRepository.save(WordLearning.start(member, word)));

        if (isDuplicateAttempt(member.getId(), request.attemptId())) {
            return toResponse(wordId, request.accuracy(), learning, false, isReview, isWithinTime(isReview, request), today);
        }

        boolean newlyMastered = learning.recordPractice(request.accuracy());
        if (isAssignedWord && newlyMastered) {
            today.addWords(1);
        }

        boolean withinTime = isWithinTime(isReview, request);
        if (isReview
                && request.accuracy() >= LearningPolicy.MASTERY_THRESHOLD
                && withinTime) {
            today.completeReview(wordId);
        }

        return toResponse(wordId, request.accuracy(), learning, newlyMastered, isReview, withinTime, today);
    }

    private boolean isDuplicateAttempt(Long memberId, String attemptId) {
        if (attemptId == null || attemptId.isBlank()) {
            return false;
        }
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(ATTEMPT_KEY_PREFIX + memberId + ":" + attemptId, "1", Duration.ofHours(24));
        return Boolean.FALSE.equals(first);
    }

    private boolean isWithinTime(boolean isReview, SubmitWordAttemptRequest request) {
        if (!isReview) {
            return true;
        }
        return request.elapsedSeconds() <= LearningPolicy.REVIEW_TIME_LIMIT_SECONDS;
    }

    private WordAttemptResponse toResponse(
            Long wordId,
            float accuracy,
            WordLearning learning,
            boolean newlyMastered,
            boolean isReview,
            boolean withinTime,
            DailyLearningResult today
    ) {
        boolean canProceed = isReview
                ? today.isReviewCompleted(wordId)
                : Boolean.TRUE.equals(learning.getIsMastered());
        return new WordAttemptResponse(
                wordId,
                accuracy,
                learning.getBestAccuracy(),
                learning.getAttemptCount(),
                Boolean.TRUE.equals(learning.getIsMastered()),
                newlyMastered,
                isReview,
                withinTime,
                canProceed
        );
    }
}
