package org.gh7035.ieumson.domain.progress.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.WordLearning;
import org.gh7035.ieumson.domain.progress.domain.repository.WordLearningRepository;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.TodayLearningResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.TodayLearningResponse.LearningStep;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.TodayLearningResponse.SentenceOptionResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.TodayLearningResponse.TodayProgressResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.TodayLearningResponse.TodayReviewResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.TodayLearningResponse.TodaySentenceResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.TodayLearningResponse.TodayWordResponse;
import org.gh7035.ieumson.domain.study.domain.Sentence;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.domain.study.domain.repository.SentenceRepository;
import org.gh7035.ieumson.domain.study.domain.repository.WordRepository;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTodayLearningService {

    private final CurrentMemberFinder currentMemberFinder;
    private final TodaySessionService todaySessionService;
    private final WordRepository wordRepository;
    private final SentenceRepository sentenceRepository;
    private final WordLearningRepository wordLearningRepository;

    @Transactional
    public TodayLearningResponse execute(CustomUserDetails userDetails) {
        Member member = currentMemberFinder.get(userDetails);
        DailyLearningResult today = todaySessionService.getOrCreate(member);

        Map<Long, Word> words = indexWords(today.assignedWordIds(), today.assignedReviewWordIds());
        Map<Long, WordLearning> learnings = indexLearnings(member, words.keySet());
        Map<Long, Sentence> sentences = indexSentences(today.assignedSentenceIds());
        Map<Long, Word> optionWords = indexOptionWords(sentences.values().stream().toList());

        List<TodayWordResponse> wordResponses = today.assignedWordIds().stream()
                .map(words::get)
                .filter(Objects::nonNull)
                .map(word -> toWordResponse(word, learnings.get(word.getId())))
                .toList();
        List<TodaySentenceResponse> sentenceResponses = today.assignedSentenceIds().stream()
                .map(sentences::get)
                .filter(Objects::nonNull)
                .map(sentence -> toSentenceResponse(sentence, today.isSentenceCompleted(sentence.getId()), optionWords))
                .toList();
        List<TodayReviewResponse> reviewResponses = today.assignedReviewWordIds().stream()
                .map(id -> words.get(id))
                .filter(Objects::nonNull)
                .map(word -> toReviewResponse(word, learnings.get(word.getId()), today.isReviewCompleted(word.getId())))
                .toList();

        TodayProgressResponse progress = new TodayProgressResponse(
                today.getWordsCompleted(),
                wordResponses.size(),
                today.getSentencesCompleted(),
                sentenceResponses.size(),
                today.completedReviewWordIds().size(),
                reviewResponses.size()
        );

        return new TodayLearningResponse(
                today.getLearnedDate(),
                today.getStreakCount(),
                Boolean.TRUE.equals(today.getCompleted()),
                Boolean.TRUE.equals(today.getGoldAwarded()),
                resolveStep(today, wordResponses),
                wordResponses,
                sentenceResponses,
                reviewResponses,
                progress
        );
    }

    private LearningStep resolveStep(DailyLearningResult today, List<TodayWordResponse> words) {
        if (Boolean.TRUE.equals(today.getCompleted())) {
            return LearningStep.DONE;
        }
        boolean wordsDone = words.stream().allMatch(TodayWordResponse::isMastered);
        if (!wordsDone) {
            return LearningStep.WORD;
        }
        if (!today.completedSentenceIds().containsAll(today.assignedSentenceIds())) {
            return LearningStep.SENTENCE;
        }
        if (!today.completedReviewWordIds().containsAll(today.assignedReviewWordIds())) {
            return LearningStep.REVIEW;
        }
        return LearningStep.COMPLETE;
    }

    private Map<Long, Word> indexWords(List<Long> assigned, List<Long> reviews) {
        List<Long> ids = java.util.stream.Stream.concat(assigned.stream(), reviews.stream()).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return wordRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Word::getId, Function.identity()));
    }

    private Map<Long, WordLearning> indexLearnings(Member member, java.util.Set<Long> wordIds) {
        if (wordIds.isEmpty()) {
            return Map.of();
        }
        return wordLearningRepository.findByMemberAndWordIdIn(member, wordIds).stream()
                .collect(Collectors.toMap(learning -> learning.getWord().getId(), Function.identity()));
    }

    private Map<Long, Sentence> indexSentences(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return sentenceRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Sentence::getId, Function.identity()));
    }

    private Map<Long, Word> indexOptionWords(List<Sentence> sentences) {
        List<Long> optionIds = sentences.stream()
                .flatMap(sentence -> sentence.getOptions().stream())
                .distinct()
                .toList();
        if (optionIds.isEmpty()) {
            return Map.of();
        }
        return wordRepository.findByIdIn(optionIds).stream()
                .collect(Collectors.toMap(Word::getId, Function.identity()));
    }

    private TodayWordResponse toWordResponse(Word word, WordLearning learning) {
        return new TodayWordResponse(
                word.getId(),
                word.getWord(),
                word.getDefinition(),
                word.getVideoLink(),
                word.getCategory(),
                word.getDifficulty(),
                learning == null ? 0f : learning.getBestAccuracy(),
                learning == null ? 0 : learning.getAttemptCount(),
                learning != null && Boolean.TRUE.equals(learning.getIsMastered())
        );
    }

    private TodaySentenceResponse toSentenceResponse(Sentence sentence, boolean completed, Map<Long, Word> optionWords) {
        List<SentenceOptionResponse> options = sentence.getOptions().stream()
                .map(id -> id instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(id)))
                .map(optionWords::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Word::getId))
                .map(word -> new SentenceOptionResponse(word.getId(), word.getWord(), word.getVideoLink()))
                .toList();
        return new TodaySentenceResponse(sentence.getId(), sentence.getSentenceKor(), completed, options);
    }

    private TodayReviewResponse toReviewResponse(Word word, WordLearning learning, boolean completed) {
        LocalDateTime lastPracticedAt = learning == null ? null : learning.getLastPracticedAt();
        long elapsedHours = lastPracticedAt == null
                ? 0
                : Duration.between(lastPracticedAt, LocalDateTime.now()).toHours();
        return new TodayReviewResponse(
                word.getId(),
                word.getWord(),
                word.getDefinition(),
                word.getVideoLink(),
                lastPracticedAt,
                elapsedHours,
                LearningPolicy.REVIEW_TIME_LIMIT_SECONDS,
                completed
        );
    }
}
