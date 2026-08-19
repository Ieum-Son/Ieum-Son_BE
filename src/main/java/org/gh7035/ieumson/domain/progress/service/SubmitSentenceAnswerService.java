package org.gh7035.ieumson.domain.progress.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.presentation.dto.request.SubmitSentenceAnswerRequest;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.SentenceAnswerResponse;
import org.gh7035.ieumson.domain.study.domain.Sentence;
import org.gh7035.ieumson.domain.study.domain.repository.SentenceRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmitSentenceAnswerService {

    private final CurrentMemberFinder currentMemberFinder;
    private final TodaySessionService todaySessionService;
    private final SentenceRepository sentenceRepository;

    public SentenceAnswerResponse execute(CustomUserDetails userDetails, Long sentenceId, SubmitSentenceAnswerRequest request) {
        Member member = currentMemberFinder.get(userDetails);
        DailyLearningResult today = todaySessionService.getOrCreate(member);
        Sentence sentence = sentenceRepository.findById(sentenceId)
                .orElseThrow(() -> new IeumException(ErrorCode.SENTENCE_NOT_FOUND));

        if (!today.hasAssignedSentence(sentenceId)) {
            throw new IeumException(ErrorCode.SENTENCE_NOT_IN_TODAY_SESSION);
        }

        Long answerWordId = sentence.getBlankWord().getId();
        boolean correct = answerWordId.equals(request.selectedWordId());
        if (correct && !today.isSentenceCompleted(sentenceId)) {
            today.completeSentence(sentenceId);
        }

        return new SentenceAnswerResponse(
                sentenceId,
                correct,
                answerWordId,
                today.isSentenceCompleted(sentenceId)
        );
    }
}
