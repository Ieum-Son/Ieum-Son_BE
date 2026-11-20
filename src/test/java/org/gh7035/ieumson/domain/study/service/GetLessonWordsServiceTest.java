package org.gh7035.ieumson.domain.study.service;

import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.enums.Role;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.WordLearning;
import org.gh7035.ieumson.domain.progress.domain.repository.WordLearningRepository;
import org.gh7035.ieumson.domain.study.domain.Chapter;
import org.gh7035.ieumson.domain.study.domain.Lesson;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.domain.study.domain.repository.LessonRepository;
import org.gh7035.ieumson.domain.study.domain.repository.WordRepository;
import org.gh7035.ieumson.domain.study.presentation.dto.response.LessonWordsResponse;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetLessonWordsServiceTest {

    private final CurrentMemberFinder currentMemberFinder = mock(CurrentMemberFinder.class);
    private final LessonRepository lessonRepository = mock(LessonRepository.class);
    private final WordRepository wordRepository = mock(WordRepository.class);
    private final WordLearningRepository wordLearningRepository = mock(WordLearningRepository.class);
    private final GetLessonWordsService getLessonWordsService = new GetLessonWordsService(
            currentMemberFinder, lessonRepository, wordRepository, wordLearningRepository);

    private Member member;
    private CustomUserDetails userDetails;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("dino@gmail.com")
                .loginId("dino")
                .name("홍길동")
                .password("encoded")
                .emailVerified(true)
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(member, "id", 10L);
        userDetails = new CustomUserDetails(member);
        when(currentMemberFinder.get(userDetails)).thenReturn(member);

        Chapter chapter = Chapter.builder()
                .title("기초")
                .description("기초 수화")
                .orderNumber(1)
                .build();
        ReflectionTestUtils.setField(chapter, "id", 1L);
        lesson = Lesson.builder()
                .name("인사")
                .chapter(chapter)
                .orderNumber(1)
                .description("인사 표현")
                .build();
        ReflectionTestUtils.setField(lesson, "id", 3L);
    }

    @Test
    void returnsLessonWordsWithMasteredFlags() {
        Word hello = word(12L, "안녕");
        Word thanks = word(13L, "고마워");
        when(lessonRepository.findWithChapterById(3L)).thenReturn(Optional.of(lesson));
        when(wordRepository.findByLessonIdOrderByIdAsc(3L)).thenReturn(List.of(hello, thanks));
        when(wordLearningRepository.findByMemberAndWordIdIn(member, Set.of(12L, 13L)))
                .thenReturn(List.of(mastered(hello)));

        LessonWordsResponse response = getLessonWordsService.execute(userDetails, 3L);

        assertThat(response.lessonId()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("인사");
        assertThat(response.chapterId()).isEqualTo(1L);
        assertThat(response.words()).hasSize(2);
        assertThat(response.words().getFirst().word()).isEqualTo("안녕");
        assertThat(response.words().getFirst().isMastered()).isTrue();
        assertThat(response.words().get(1).word()).isEqualTo("고마워");
        assertThat(response.words().get(1).isMastered()).isFalse();
    }

    @Test
    void throwsWhenLessonDoesNotExist() {
        when(lessonRepository.findWithChapterById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getLessonWordsService.execute(userDetails, 99L))
                .isInstanceOf(IeumException.class)
                .extracting(thrown -> ((IeumException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.LESSON_NOT_FOUND);
    }

    private Word word(Long id, String label) {
        Word word = Word.builder()
                .word(label)
                .definition(label)
                .videoLink("https://example.com/" + id)
                .category("daily")
                .difficulty(1)
                .lesson(lesson)
                .build();
        ReflectionTestUtils.setField(word, "id", id);
        return word;
    }

    private WordLearning mastered(Word word) {
        WordLearning learning = WordLearning.start(member, word);
        learning.recordPractice(95f);
        return learning;
    }
}
