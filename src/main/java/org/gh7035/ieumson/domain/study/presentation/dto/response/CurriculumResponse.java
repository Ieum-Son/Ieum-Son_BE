package org.gh7035.ieumson.domain.study.presentation.dto.response;

import java.util.List;

public record CurriculumResponse(
        Long currentChapterId,
        List<ChapterResponse> chapters
) {
    public record ChapterResponse(
            Long chapterId,
            String title,
            String description,
            int orderNumber,
            boolean isCurrent,
            List<LessonResponse> lessons
    ) {
    }

    public record LessonResponse(
            Long lessonId,
            String name,
            String description,
            int orderNumber,
            int wordCount,
            int masteredWordCount
    ) {
    }
}
