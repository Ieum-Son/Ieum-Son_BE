package org.gh7035.ieumson.domain.study.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.WordLearning;
import org.gh7035.ieumson.domain.progress.domain.repository.WordLearningRepository;
import org.gh7035.ieumson.domain.study.domain.Chapter;
import org.gh7035.ieumson.domain.study.domain.Lesson;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.domain.study.domain.repository.ChapterRepository;
import org.gh7035.ieumson.domain.study.domain.repository.LessonRepository;
import org.gh7035.ieumson.domain.study.domain.repository.WordRepository;
import org.gh7035.ieumson.domain.study.presentation.dto.response.CurriculumResponse;
import org.gh7035.ieumson.domain.study.presentation.dto.response.CurriculumResponse.ChapterResponse;
import org.gh7035.ieumson.domain.study.presentation.dto.response.CurriculumResponse.LessonResponse;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCurriculumService {

    private final CurrentMemberFinder currentMemberFinder;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final WordRepository wordRepository;
    private final WordLearningRepository wordLearningRepository;

    public CurriculumResponse execute(CustomUserDetails userDetails) {
        Member member = currentMemberFinder.get(userDetails);
        Set<Long> masteredWordIds = wordLearningRepository.findByMemberAndIsMasteredTrue(member).stream()
                .map(WordLearning::getWord)
                .map(Word::getId)
                .collect(Collectors.toSet());

        List<Chapter> chapters = chapterRepository.findAllByOrderByOrderNumberAsc();
        List<Lesson> lessons = lessonRepository.findAll();
        List<Word> words = wordRepository.findAll();

        Map<Long, List<Lesson>> lessonsByChapter = lessons.stream()
                .collect(Collectors.groupingBy(lesson -> lesson.getChapter().getId()));
        Map<Long, List<Word>> wordsByLesson = words.stream()
                .collect(Collectors.groupingBy(word -> word.getLesson().getId()));

        Long currentChapterId = resolveCurrentChapterId(chapters, lessonsByChapter, wordsByLesson, masteredWordIds);

        List<ChapterResponse> chapterResponses = chapters.stream()
                .map(chapter -> {
                    List<LessonResponse> lessonResponses = lessonsByChapter
                            .getOrDefault(chapter.getId(), List.of())
                            .stream()
                            .sorted((a, b) -> Integer.compare(a.getOrderNumber(), b.getOrderNumber()))
                            .map(lesson -> toLesson(lesson, wordsByLesson.getOrDefault(lesson.getId(), List.of()), masteredWordIds))
                            .toList();
                    return new ChapterResponse(
                            chapter.getId(),
                            chapter.getTitle(),
                            chapter.getDescription(),
                            chapter.getOrderNumber(),
                            chapter.getId().equals(currentChapterId),
                            lessonResponses
                    );
                })
                .toList();

        return new CurriculumResponse(currentChapterId, chapterResponses);
    }

    private LessonResponse toLesson(Lesson lesson, List<Word> lessonWords, Set<Long> masteredWordIds) {
        int mastered = (int) lessonWords.stream()
                .filter(word -> masteredWordIds.contains(word.getId()))
                .count();
        return new LessonResponse(
                lesson.getId(),
                lesson.getName(),
                lesson.getDescription(),
                lesson.getOrderNumber(),
                lessonWords.size(),
                mastered
        );
    }

    private Long resolveCurrentChapterId(
            List<Chapter> chapters,
            Map<Long, List<Lesson>> lessonsByChapter,
            Map<Long, List<Word>> wordsByLesson,
            Set<Long> masteredWordIds
    ) {
        for (Chapter chapter : chapters) {
            boolean allMastered = lessonsByChapter.getOrDefault(chapter.getId(), List.of()).stream()
                    .flatMap(lesson -> wordsByLesson.getOrDefault(lesson.getId(), List.of()).stream())
                    .allMatch(word -> masteredWordIds.contains(word.getId()));
            boolean hasWords = lessonsByChapter.getOrDefault(chapter.getId(), List.of()).stream()
                    .anyMatch(lesson -> !wordsByLesson.getOrDefault(lesson.getId(), List.of()).isEmpty());
            if (hasWords && !allMastered) {
                return chapter.getId();
            }
        }
        return chapters.isEmpty() ? null : chapters.getLast().getId();
    }
}
