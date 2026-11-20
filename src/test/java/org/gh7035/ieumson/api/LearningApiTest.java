package org.gh7035.ieumson.api;

import com.jayway.jsonpath.JsonPath;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.study.domain.Lesson;
import org.gh7035.ieumson.domain.study.domain.Sentence;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.domain.study.domain.repository.SentenceRepository;
import org.gh7035.ieumson.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LearningApiTest extends ApiTestSupport {

    @Autowired
    private SentenceRepository sentenceRepository;

    @Test
    void dailySentenceAnswerWordAttemptAndComplete() throws Exception {
        String loginId = uid("learn");
        Member member = saveMember(loginId);
        Lesson lesson = seedLesson("기초", 1, "인사");
        Word answer = seedWord(lesson, "가능");
        Word wrong = seedWord(lesson, "같다");
        Word extra = seedWord(lesson, "가입");
        Sentence sentence = sentenceRepository.save(Sentence.of(
                lesson,
                "___ 하세요",
                answer,
                Arrays.asList(answer.getId(), wrong.getId(), extra.getId()),
                1
        ));
        String authorization = bearer(loginId);

        MvcResult daily = mockMvc.perform(get("/api/progress/daily").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isNumber())
                .andExpect(jsonPath("$.sessionTicket").isString())
                .andExpect(jsonPath("$.streakCount").value(0))
                .andExpect(jsonPath("$.words.length()").value(3))
                .andExpect(jsonPath("$.sentences.length()").value(1))
                .andExpect(jsonPath("$.sentences[0].sentenceId").value(sentence.getId()))
                .andExpect(jsonPath("$.reviews.length()").value(0))
                .andExpect(jsonPath("$.progress.wordsTotal").value(3))
                .andExpect(jsonPath("$.progress.sentencesTotal").value(1))
                .andReturn();
        long sessionId = ((Number) JsonPath.read(daily.getResponse().getContentAsString(), "$.sessionId")).longValue();

        mockMvc.perform(get("/api/members/me").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streakCount").value(0));

        mockMvc.perform(post("/api/progress/sentences/{sentenceId}/answers", sentence.getId())
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedWordId":%d}
                                """.formatted(wrong.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentenceId").value(sentence.getId()))
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.answerWordId").value(answer.getId()))
                .andExpect(jsonPath("$.isCompleted").value(false));

        mockMvc.perform(post("/api/progress/complete").header("Authorization", authorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("오늘의 학습을 모두 완료해야 합니다."));

        mockMvc.perform(post("/api/progress/sentences/{sentenceId}/answers", sentence.getId())
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedWordId":%d}
                                """.formatted(answer.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.isCompleted").value(true));

        mockMvc.perform(post("/internal/words/{wordId}/attempts", answer.getId())
                        .header("Authorization", authorization))
                .andExpect(status().isUnauthorized());

        for (Word word : List.of(answer, wrong, extra)) {
            mockMvc.perform(post("/internal/words/{wordId}/attempts", word.getId())
                            .header("Authorization", attemptToken(member.getId(), sessionId, word.getId(), true)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.wordId").value(word.getId()))
                    .andExpect(jsonPath("$.passed").value(true));
        }

        mockMvc.perform(post("/api/progress/complete").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streakCount").value(1))
                .andExpect(jsonPath("$.completedDayCount").value(1))
                .andExpect(jsonPath("$.goldFromWords").value(3))
                .andExpect(jsonPath("$.streakBonus").value(0))
                .andExpect(jsonPath("$.goldEarned").value(3))
                .andExpect(jsonPath("$.goldBalance").value(3))
                .andExpect(jsonPath("$.wordsCompleted").value(3))
                .andExpect(jsonPath("$.sentencesCompleted").value(1));

        mockMvc.perform(get("/api/members/me").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streakCount").value(1));

        mockMvc.perform(post("/api/progress/complete").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streakCount").value(1))
                .andExpect(jsonPath("$.completedDayCount").value(1))
                .andExpect(jsonPath("$.goldEarned").value(0))
                .andExpect(jsonPath("$.goldBalance").value(3));
    }

    @Test
    void yesterdayCompletedCarriesStreakAndAwardsBonus() throws Exception {
        String loginId = uid("carry");
        Member member = saveMember(loginId);
        savePastLearning(member, LocalDate.now(LearningPolicy.ZONE).minusDays(1), 4, true);

        Lesson lesson = seedLesson("기초", 1, "인사");
        Word answer = seedWord(lesson, "가능");
        Word wrong = seedWord(lesson, "같다");
        Word extra = seedWord(lesson, "가입");
        Sentence sentence = sentenceRepository.save(Sentence.of(
                lesson,
                "___ 하세요",
                answer,
                Arrays.asList(answer.getId(), wrong.getId(), extra.getId()),
                1
        ));
        String authorization = bearer(loginId);

        MvcResult daily = mockMvc.perform(get("/api/progress/daily").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streakCount").value(4))
                .andReturn();
        long sessionId = ((Number) JsonPath.read(daily.getResponse().getContentAsString(), "$.sessionId")).longValue();

        mockMvc.perform(post("/api/progress/sentences/{sentenceId}/answers", sentence.getId())
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedWordId":%d}
                                """.formatted(answer.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));

        for (Word word : List.of(answer, wrong, extra)) {
            mockMvc.perform(post("/internal/words/{wordId}/attempts", word.getId())
                            .header("Authorization", attemptToken(member.getId(), sessionId, word.getId(), true)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.passed").value(true));
        }

        mockMvc.perform(post("/api/progress/complete").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streakCount").value(5))
                .andExpect(jsonPath("$.goldFromWords").value(3))
                .andExpect(jsonPath("$.streakBonus").value(greaterThan(0)))
                .andExpect(jsonPath("$.goldEarned").value(greaterThanOrEqualTo(3 + LearningPolicy.STREAK_BONUS_MIN)));
    }

    @Test
    void submitAndCompleteWithoutDailyReturn404() throws Exception {
        String loginId = uid("nosession");
        saveMember(loginId);
        Lesson lesson = seedLesson("기초", 1, "인사");
        Word answer = seedWord(lesson, "가능");
        Word wrong = seedWord(lesson, "같다");
        Word extra = seedWord(lesson, "가입");
        Sentence sentence = sentenceRepository.save(Sentence.of(
                lesson,
                "___ 하세요",
                answer,
                Arrays.asList(answer.getId(), wrong.getId(), extra.getId()),
                1
        ));
        String authorization = bearer(loginId);

        mockMvc.perform(post("/api/progress/sentences/{sentenceId}/answers", sentence.getId())
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedWordId":%d}
                                """.formatted(answer.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("오늘 학습이 없습니다."));

        mockMvc.perform(post("/api/progress/complete").header("Authorization", authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("오늘 학습이 없습니다."));
    }

    @Test
    void sentenceAndWordOutsideTodaySessionAreRejected() throws Exception {
        String loginId = uid("outside");
        Member member = saveMember(loginId);
        Lesson first = seedLesson("기초", 1, "인사");
        for (String label : java.util.List.of("가능", "같다", "가입", "가정", "가지다")) {
            seedWord(first, label);
        }
        Lesson second = seedLesson("심화", 2, "가족");
        Word outside = seedWord(second, "가족");
        Word optionA = seedWord(second, "엄마");
        Word optionB = seedWord(second, "아빠");
        Sentence sentence = sentenceRepository.save(Sentence.of(
                second,
                "우리 ___",
                outside,
                Arrays.asList(outside.getId(), optionA.getId(), optionB.getId()),
                1
        ));
        String authorization = bearer(loginId);

        MvcResult daily = mockMvc.perform(get("/api/progress/daily").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.words.length()").value(5))
                .andExpect(jsonPath("$.sentences.length()").value(0))
                .andReturn();
        long sessionId = ((Number) JsonPath.read(daily.getResponse().getContentAsString(), "$.sessionId")).longValue();

        mockMvc.perform(post("/internal/words/{wordId}/attempts", outside.getId())
                        .header("Authorization", attemptToken(member.getId(), sessionId, outside.getId(), true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("오늘 학습에 배정되지 않은 단어입니다."));

        mockMvc.perform(post("/internal/words/{wordId}/attempts", 999999L)
                        .header("Authorization", attemptToken(member.getId(), sessionId, 999999L, false)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 단어가 존재하지 않습니다."));

        mockMvc.perform(post("/api/progress/sentences/{sentenceId}/answers", sentence.getId())
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedWordId":%d}
                                """.formatted(outside.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("오늘 학습에 배정되지 않은 문장입니다."));

        mockMvc.perform(post("/api/progress/sentences/{sentenceId}/answers", 999999L)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedWordId":1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 문장이 존재하지 않습니다."));
    }
}
