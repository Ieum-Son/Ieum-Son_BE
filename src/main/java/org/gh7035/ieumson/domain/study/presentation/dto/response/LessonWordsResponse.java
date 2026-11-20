package org.gh7035.ieumson.domain.study.presentation.dto.response;

import java.util.List;

public record LessonWordsResponse(
        Long lessonId,
        String name,
        String description,
        Long chapterId,
        List<LessonWordResponse> words
) {
    public record LessonWordResponse(
            Long wordId,
            String word,
            String definition,
            String videoUrl,
            String category,
            int difficulty,
            boolean isMastered
    ) {
    }
}
