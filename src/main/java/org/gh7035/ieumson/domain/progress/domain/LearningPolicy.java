package org.gh7035.ieumson.domain.progress.domain;

import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;

public final class LearningPolicy {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    public static final int DAILY_WORD_COUNT = 5;
    public static final float MASTERY_THRESHOLD = 90f;
    public static final int GOLD_PER_WORD = 1;
    public static final int STREAK_RECOVERY_COST = 35;
    public static final int STREAK_BONUS_MIN = 2;
    public static final int STREAK_BONUS_MAX = 3;
    public static final int REVIEW_COUNT = 3;
    public static final int REVIEW_TIME_LIMIT_SECONDS = 15;

    private LearningPolicy() {
    }

    public static int randomStreakBonus() {
        return ThreadLocalRandom.current().nextInt(STREAK_BONUS_MIN, STREAK_BONUS_MAX + 1);
    }

    public static int calendarIntensity(int wordsCompleted, int sentencesCompleted) {
        int activity = wordsCompleted + sentencesCompleted;
        if (activity <= 0) {
            return 0;
        }
        if (activity <= 2) {
            return 1;
        }
        if (activity <= 5) {
            return 2;
        }
        if (activity <= 8) {
            return 3;
        }
        return 4;
    }
}
