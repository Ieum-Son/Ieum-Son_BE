package org.gh7035.ieumson.api;

import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.progress.domain.WordLearning;
import org.gh7035.ieumson.domain.progress.domain.repository.WordLearningRepository;
import org.gh7035.ieumson.domain.study.domain.Lesson;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudyApiTest extends ApiTestSupport {

    @Autowired
    private WordLearningRepository wordLearningRepository;

    @Test
    void curriculumAndLessonWords() throws Exception {
        String loginId = uid("study");
        Member member = saveMember(loginId);
        Lesson lesson = seedLesson("기초", 1, "인사");
        Word mastered = seedWord(lesson, "안녕");
        seedWord(lesson, "감사");
        WordLearning learning = WordLearning.start(member, mastered);
        learning.recordPractice(95f);
        wordLearningRepository.save(learning);

        String authorization = bearer(loginId);

        mockMvc.perform(get("/api/study/chapters").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentChapterId").value(lesson.getChapter().getId()))
                .andExpect(jsonPath("$.chapters.length()").value(1))
                .andExpect(jsonPath("$.chapters[0].title").value("기초"))
                .andExpect(jsonPath("$.chapters[0].isCurrent").value(true))
                .andExpect(jsonPath("$.chapters[0].lessons[0].name").value("인사"))
                .andExpect(jsonPath("$.chapters[0].lessons[0].wordCount").value(2))
                .andExpect(jsonPath("$.chapters[0].lessons[0].masteredWordCount").value(1));

        mockMvc.perform(get("/api/study/lessons/{lessonId}", lesson.getId())
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(lesson.getId()))
                .andExpect(jsonPath("$.name").value("인사"))
                .andExpect(jsonPath("$.chapterId").value(lesson.getChapter().getId()))
                .andExpect(jsonPath("$.words.length()").value(2))
                .andExpect(jsonPath("$.words[0].word").value("안녕"))
                .andExpect(jsonPath("$.words[0].definition").value("안녕 뜻"))
                .andExpect(jsonPath("$.words[0].isMastered").value(true))
                .andExpect(jsonPath("$.words[1].word").value("감사"))
                .andExpect(jsonPath("$.words[1].isMastered").value(false));
    }

    @Test
    void lessonNotFound() throws Exception {
        String loginId = uid("missing");
        saveMember(loginId);

        mockMvc.perform(get("/api/study/lessons/{lessonId}", 999999L)
                        .header("Authorization", bearer(loginId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 레슨이 존재하지 않습니다."));
    }
}
