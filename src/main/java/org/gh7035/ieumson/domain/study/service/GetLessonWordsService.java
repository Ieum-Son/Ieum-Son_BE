package org.gh7035.ieumson.domain.study.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.WordLearning;
import org.gh7035.ieumson.domain.progress.domain.repository.WordLearningRepository;
import org.gh7035.ieumson.domain.study.domain.Lesson;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.domain.study.domain.repository.LessonRepository;
import org.gh7035.ieumson.domain.study.domain.repository.WordRepository;
import org.gh7035.ieumson.domain.study.presentation.dto.response.LessonWordsResponse;
import org.gh7035.ieumson.domain.study.presentation.dto.response.LessonWordsResponse.LessonWordResponse;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetLessonWordsService {

    private final CurrentMemberFinder currentMemberFinder;
    private final LessonRepository lessonRepository;
    private final WordRepository wordRepository;
    private final WordLearningRepository wordLearningRepository;

    public LessonWordsResponse execute(CustomUserDetails userDetails, Long lessonId) {
        Member member = currentMemberFinder.get(userDetails);
        Lesson lesson = lessonRepository.findWithChapterById(lessonId)
                .orElseThrow(() -> new IeumException(ErrorCode.LESSON_NOT_FOUND));

        List<Word> words = wordRepository.findByLessonIdOrderByIdAsc(lessonId);
        Set<Long> wordIds = words.stream().map(Word::getId).collect(Collectors.toSet());
        Set<Long> masteredWordIds = wordIds.isEmpty()
                ? Set.of()
                : wordLearningRepository.findByMemberAndWordIdIn(member, wordIds).stream()
                .filter(learning -> Boolean.TRUE.equals(learning.getIsMastered()))
                .map(learning -> learning.getWord().getId())
                .collect(Collectors.toSet());

        List<LessonWordResponse> wordResponses = words.stream()
                .map(word -> new LessonWordResponse(
                        word.getId(),
                        word.getWord(),
                        word.getDefinition(),
                        word.getVideoLink(),
                        word.getCategory(),
                        word.getDifficulty(),
                        masteredWordIds.contains(word.getId())
                ))
                .toList();

        return new LessonWordsResponse(
                lesson.getId(),
                lesson.getName(),
                lesson.getDescription(),
                lesson.getChapter().getId(),
                wordResponses
        );
    }
}
