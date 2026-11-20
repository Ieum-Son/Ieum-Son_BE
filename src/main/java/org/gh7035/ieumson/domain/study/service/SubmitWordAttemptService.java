package org.gh7035.ieumson.domain.study.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.WordLearning;
import org.gh7035.ieumson.domain.progress.domain.repository.DailyLearningResultRepository;
import org.gh7035.ieumson.domain.progress.domain.repository.WordLearningRepository;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.domain.study.domain.repository.WordRepository;
import org.gh7035.ieumson.domain.study.presentation.dto.response.WordAttemptResponse;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.infrastructure.ticket.AttemptClaims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmitWordAttemptService {

    private final MemberRepository memberRepository;
    private final DailyLearningResultRepository dailyLearningResultRepository;
    private final WordRepository wordRepository;
    private final WordLearningRepository wordLearningRepository;

    public WordAttemptResponse execute(AttemptClaims attempt) {
        Member member = memberRepository.findById(attempt.memberId())
                .orElseThrow(() -> new IeumException(ErrorCode.INVALID_TOKEN));
        DailyLearningResult session = dailyLearningResultRepository.findById(attempt.sessionId())
                .orElseThrow(() -> new IeumException(ErrorCode.WORD_NOT_IN_TODAY_SESSION));
        if (!session.getMember().getId().equals(member.getId())
                || !session.getLearnedDate().equals(LocalDate.now(LearningPolicy.ZONE))) {
            throw new IeumException(ErrorCode.WORD_NOT_IN_TODAY_SESSION);
        }

        Word word = wordRepository.findById(attempt.wordId())
                .orElseThrow(() -> new IeumException(ErrorCode.WORD_NOT_FOUND));
        boolean isReview = session.hasAssignedReview(word.getId());
        boolean isAssignedWord = session.hasAssignedWord(word.getId());
        if (!isAssignedWord && !isReview) {
            throw new IeumException(ErrorCode.WORD_NOT_IN_TODAY_SESSION);
        }
        if (!attempt.correct()) {
            return new WordAttemptResponse(word.getId(), false);
        }

        WordLearning learning = wordLearningRepository.findByMemberAndWord(member, word)
                .orElseGet(() -> wordLearningRepository.save(WordLearning.start(member, word)));
        boolean newlyPassed = learning.markPassed();
        if (isAssignedWord && newlyPassed) {
            session.addWords(1);
        }
        if (isReview) {
            session.completeReview(word.getId());
        }
        return new WordAttemptResponse(word.getId(), true);
    }
}
