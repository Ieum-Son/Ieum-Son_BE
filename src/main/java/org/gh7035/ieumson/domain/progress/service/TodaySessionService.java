package org.gh7035.ieumson.domain.progress.service;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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

    @Transactional
    public DailyLearningResult getOrCreate(Member member) {
        LocalDate today = LocalDate.now(LearningPolicy.ZONE);
        return dailyLearningResultRepository.findByMemberAndLearnedDate(member, today)
                .orElseGet(() -> createToday(member, today));
    }

    private DailyLearningResult createToday(Member member, LocalDate today) {
        int streakCount = resolveStreak(member, today);
        List<Word> assignedWords = assignWords(member);
        List<Sentence> assignedSentences = assignSentences(assignedWords);
        List<Word> reviewWords = assignReviews(member, assignedWords, today);

        DailyLearningResult created = DailyLearningResult.start(
                member,
                today,
                streakCount,
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

    private int resolveStreak(Member member, LocalDate today) {
        return dailyLearningResultRepository.findTopByMemberOrderByLearnedDateDesc(member)
                .map(last -> {
                    if (last.getLearnedDate().equals(today.minusDays(1))) {
                        return last.getStreakCount() + 1;
                    }
                    if (last.getLearnedDate().equals(today)) {
                        return last.getStreakCount();
                    }
                    return 1;
                })
                .orElse(1);
    }

    private List<Word> assignWords(Member member) {
        Set<Long> masteredIds = wordLearningRepository.findByMemberAndIsMasteredTrue(member).stream()
                .map(learning -> learning.getWord().getId())
                .collect(Collectors.toSet());

        List<Word> remaining = wordRepository.findAllInCurriculumOrder().stream()
                .filter(word -> !masteredIds.contains(word.getId()))
                .limit(LearningPolicy.DAILY_WORD_COUNT)
                .toList();

        return remaining;
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
        return candidates.stream()
                .limit(LearningPolicy.REVIEW_COUNT)
                .toList();
    }
}
