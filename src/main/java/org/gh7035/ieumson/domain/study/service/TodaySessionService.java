package org.gh7035.ieumson.domain.study.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.WordLearning;
import org.gh7035.ieumson.domain.progress.domain.repository.DailyLearningResultRepository;
import org.gh7035.ieumson.domain.progress.domain.repository.WordLearningRepository;
import org.gh7035.ieumson.domain.study.domain.Lesson;
import org.gh7035.ieumson.domain.study.domain.Sentence;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.domain.study.domain.repository.SentenceRepository;
import org.gh7035.ieumson.domain.study.domain.repository.WordRepository;
import org.gh7035.ieumson.infrastructure.detect.DetectLabelVocabulary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TodaySessionService {

    private final DailyLearningResultRepository dailyLearningResultRepository;
    private final WordLearningRepository wordLearningRepository;
    private final WordRepository wordRepository;
    private final SentenceRepository sentenceRepository;
    private final DetectLabelVocabulary detectLabelVocabulary;

    @Transactional(readOnly = true)
    public Optional<DailyLearningResult> findToday(Member member) {
        LocalDate today = LocalDate.now(LearningPolicy.ZONE);
        return dailyLearningResultRepository.findByMemberAndLearnedDate(member, today);
    }

    @Transactional
    public DailyLearningResult createToday(Member member) {
        LocalDate today = LocalDate.now(LearningPolicy.ZONE);
        Optional<DailyLearningResult> existing =
                dailyLearningResultRepository.findByMemberAndLearnedDate(member, today);
        if (existing.isPresent()) {
            return existing.get();
        }

        List<Word> assignedWords = assignWords(member);
        List<Sentence> assignedSentences = assignSentences(assignedWords);
        List<Word> reviewWords = assignReviews(member, assignedWords, today);

        DailyLearningResult created = DailyLearningResult.start(
                member,
                today,
                0,
                assignedWords.stream().map(Word::getId).toList(),
                assignedSentences.stream().map(Sentence::getId).toList(),
                reviewWords.stream().map(Word::getId).toList()
        );

        try {
            return dailyLearningResultRepository.saveAndFlush(created);
        } catch (DataIntegrityViolationException e) {
            return dailyLearningResultRepository.findByMemberAndLearnedDate(member, today)
                    .orElseThrow(() -> e);
        }
    }

    private List<Word> assignWords(Member member) {
        Set<Long> masteredIds = wordLearningRepository.findByMemberAndIsMasteredTrue(member).stream()
                .map(learning -> learning.getWord().getId())
                .collect(Collectors.toSet());

        List<Word> remaining = wordRepository.findAllInCurriculumOrder().stream()
                .filter(word -> !masteredIds.contains(word.getId()))
                .toList();
        return onlyDetectable(remaining, LearningPolicy.DAILY_WORD_COUNT);
    }

    private List<Sentence> assignSentences(List<Word> assignedWords) {
        if (assignedWords.isEmpty()) {
            return List.of();
        }
        Set<Lesson> lessons = assignedWords.stream()
                .map(Word::getLesson)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return sentenceRepository.findByLessonInOrderByOrderNumAsc(lessons);
    }

    private List<Word> assignReviews(Member member, List<Word> assignedWords, LocalDate today) {
        Set<Long> todayWordIds = assignedWords.stream()
                .map(Word::getId)
                .collect(Collectors.toSet());

        List<Word> candidates = new ArrayList<>(
                wordLearningRepository.findByMemberAndIsMasteredTrue(member).stream()
                        .map(WordLearning::getWord)
                        .filter(word -> !todayWordIds.contains(word.getId()))
                        .toList()
        );
        if (candidates.isEmpty()) {
            return List.of();
        }
        Collections.shuffle(candidates, new Random(member.getId() + today.toEpochDay()));
        return onlyDetectable(candidates, LearningPolicy.REVIEW_COUNT);
    }

    private List<Word> onlyDetectable(List<Word> words, int limit) {
        return words.stream()
                .filter(detectLabelVocabulary::matches)
                .limit(limit)
                .toList();
    }
}
