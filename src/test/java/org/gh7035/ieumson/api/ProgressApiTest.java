package org.gh7035.ieumson.api;

import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.progress.domain.GoldTransaction;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.WordLearning;
import org.gh7035.ieumson.domain.progress.domain.enums.GoldTransactionType;
import org.gh7035.ieumson.domain.progress.domain.repository.GoldTransactionRepository;
import org.gh7035.ieumson.domain.progress.domain.repository.WordLearningRepository;
import org.gh7035.ieumson.domain.study.domain.Lesson;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProgressApiTest extends ApiTestSupport {

    @Autowired
    private GoldTransactionRepository goldTransactionRepository;
    @Autowired
    private WordLearningRepository wordLearningRepository;

    @Test
    void streakCalendarGoldAndRanking() throws Exception {
        String loginId = uid("me");
        Member me = saveMember(loginId);
        me.addGold(10);
        LocalDate learnedOn = LocalDate.now(LearningPolicy.ZONE).minusDays(3);
        savePastLearning(me, learnedOn, 4, true);
        goldTransactionRepository.save(GoldTransaction.of(
                me, GoldTransactionType.EARN_LEARNING, 10, "학습 완료"));

        Member rival = saveMember(uid("rival"), uid("rival") + "@gmail.com", RAW_PASSWORD, true);
        rival.addGold(40);

        String authorization = bearer(loginId);

        mockMvc.perform(get("/api/streak").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(0))
                .andExpect(jsonPath("$.goldBalance").value(10))
                .andExpect(jsonPath("$.recoverable").value(true))
                .andExpect(jsonPath("$.recoveryCost").value(LearningPolicy.STREAK_RECOVERY_COST))
                .andExpect(jsonPath("$.week.length()").value(7))
                .andExpect(jsonPath("$.week[?(@.date == '" + learnedOn + "')].wordsCompleted", hasItem(1)))
                .andExpect(jsonPath("$.week[?(@.date == '" + LocalDate.now(LearningPolicy.ZONE) + "')].wordsCompleted", hasItem(0)))
                .andExpect(jsonPath("$.week[0].intensity").doesNotExist());

        mockMvc.perform(get("/api/streak/calendar")
                        .header("Authorization", authorization)
                        .param("year", String.valueOf(learnedOn.getYear()))
                        .param("month", String.valueOf(learnedOn.getMonthValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(learnedOn.getYear()))
                .andExpect(jsonPath("$.month").value(learnedOn.getMonthValue()))
                .andExpect(jsonPath("$.days[?(@.date == '" + learnedOn + "')].intensity", hasItem(1)));

        mockMvc.perform(get("/api/streak/gold").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].amount").value(10))
                .andExpect(jsonPath("$.items[0].type").value("EARN_LEARNING"))
                .andExpect(jsonPath("$.items[0].description").value("학습 완료"));

        mockMvc.perform(get("/api/ranking").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRank").value(2))
                .andExpect(jsonPath("$.myGold").value(10))
                .andExpect(jsonPath("$.totalMembers").value(2))
                .andExpect(jsonPath("$.items[0].rank").value(1))
                .andExpect(jsonPath("$.items[0].gold").value(40))
                .andExpect(jsonPath("$.items[0].isMe").value(false))
                .andExpect(jsonPath("$.items[1].rank").value(2))
                .andExpect(jsonPath("$.items[1].isMe").value(true));
    }

    @Test
    void recoverStreakSpendsGold() throws Exception {
        String loginId = uid("recover");
        Member member = saveMember(loginId);
        member.addGold(40);
        savePastLearning(member, LocalDate.now(LearningPolicy.ZONE).minusDays(3), 4, true);
        String authorization = bearer(loginId);

        mockMvc.perform(post("/api/streak/recover").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streakCount").value(5))
                .andExpect(jsonPath("$.goldBalance").value(5))
                .andExpect(jsonPath("$.spentGold").value(LearningPolicy.STREAK_RECOVERY_COST));

        mockMvc.perform(get("/api/streak/gold").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(5))
                .andExpect(jsonPath("$.items[0].type").value("SPEND_STREAK_RECOVERY"))
                .andExpect(jsonPath("$.items[0].amount").value(-LearningPolicy.STREAK_RECOVERY_COST));
    }

    @Test
    void recoverRejectsUnbrokenStreakAndInsufficientGold() throws Exception {
        String unbrokenId = uid("unbroken");
        Member unbroken = saveMember(unbrokenId);
        unbroken.addGold(40);
        savePastLearning(unbroken, LocalDate.now(LearningPolicy.ZONE).minusDays(1), 2, true);

        mockMvc.perform(post("/api/streak/recover").header("Authorization", bearer(unbrokenId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("복구할 스트릭이 없습니다."));

        String poorId = uid("poor");
        Member poor = saveMember(poorId);
        poor.addGold(10);
        savePastLearning(poor, LocalDate.now(LearningPolicy.ZONE).minusDays(3), 2, true);

        mockMvc.perform(post("/api/streak/recover").header("Authorization", bearer(poorId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("금조각이 부족합니다."));
    }

    @Test
    void favoritesAddListAndRemove() throws Exception {
        String loginId = uid("fav");
        Member member = saveMember(loginId);
        Lesson lesson = seedLesson("기초", 1, "인사");
        Word mastered = seedWord(lesson, "안녕");
        Word learningWord = seedWord(lesson, "감사");
        WordLearning masteredLearning = WordLearning.start(member, mastered);
        masteredLearning.markPassed();
        wordLearningRepository.save(masteredLearning);
        wordLearningRepository.save(WordLearning.start(member, learningWord));
        String authorization = bearer(loginId);

        mockMvc.perform(get("/api/favorites").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        mockMvc.perform(post("/api/favorites/{wordId}", learningWord.getId())
                        .header("Authorization", authorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("학습을 완료한 단어만 즐겨찾기할 수 있습니다."));

        mockMvc.perform(post("/api/favorites/{wordId}", 999999L)
                        .header("Authorization", authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 단어가 존재하지 않습니다."));

        mockMvc.perform(post("/api/favorites/{wordId}", mastered.getId())
                        .header("Authorization", authorization))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/favorites/{wordId}", mastered.getId())
                        .header("Authorization", authorization))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 즐겨찾기한 단어입니다."));

        mockMvc.perform(get("/api/favorites").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].wordId").value(mastered.getId()))
                .andExpect(jsonPath("$.items[0].word").value("안녕"))
                .andExpect(jsonPath("$.items[0].videoUrl").value("https://example.com/안녕"))
                .andExpect(jsonPath("$.items[0].category").value("daily"));

        mockMvc.perform(delete("/api/favorites/{wordId}", mastered.getId())
                        .header("Authorization", authorization))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/favorites/{wordId}", mastered.getId())
                        .header("Authorization", authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("즐겨찾기한 단어가 아닙니다."));

        mockMvc.perform(get("/api/favorites").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }
}
