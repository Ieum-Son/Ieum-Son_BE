package org.gh7035.ieumson.infrastructure.detect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 카메라 모델이 오늘 학습에 넣을 수 있는 단어.
 * {@code detect-labels.json}은 ksl_v4_expanded 평가에서 Top-1 70% 이상(A·B)인 커리큘럼 단어다.
 */
@Component
public class DetectLabelVocabulary {

    private static final TypeReference<List<String>> GLOSS_LIST = new TypeReference<>() {
    };

    private final Set<String> bareWords;

    @Autowired
    public DetectLabelVocabulary(ObjectMapper objectMapper) {
        this(load(objectMapper));
    }

    DetectLabelVocabulary(List<String> glosses) {
        this.bareWords = glosses.stream()
                .map(DetectLabelVocabulary::bare)
                .filter(word -> !word.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean matches(Word word) {
        return word != null && matches(word.getWord());
    }

    public boolean matches(String word) {
        if (word == null || word.isBlank()) {
            return false;
        }
        String normalized = word.trim();
        return bareWords.contains(normalized) || bareWords.contains(bare(normalized));
    }

    private static final Pattern SENSE_SUFFIX = Pattern.compile("#?\\d+#?$|#$");

    static String bare(String gloss) {
        String value = gloss.trim();
        String stripped = SENSE_SUFFIX.matcher(value).replaceFirst("").trim();
        return stripped.isBlank() ? value : stripped;
    }

    private static List<String> load(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource("detect-labels.json");
        if (!resource.exists()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(resource.getInputStream(), GLOSS_LIST);
        } catch (IOException e) {
            throw new UncheckedIOException("detect-labels.json을 읽지 못했습니다.", e);
        }
    }
}
