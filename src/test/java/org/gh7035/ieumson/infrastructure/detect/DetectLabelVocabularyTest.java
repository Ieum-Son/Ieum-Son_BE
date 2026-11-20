package org.gh7035.ieumson.infrastructure.detect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DetectLabelVocabularyTest {

    private final DetectLabelVocabulary vocabulary = new DetectLabelVocabulary(List.of(
            "1",
            "가능1",
            "가다1",
            "가입1",
            "곳1#",
            "학기"
    ));

    @Test
    void matchesCurriculumWordsToGlossIds() {
        assertThat(DetectLabelVocabulary.bare("가입1")).isEqualTo("가입");
        assertThat(DetectLabelVocabulary.bare("곳1#")).isEqualTo("곳");
        assertThat(DetectLabelVocabulary.bare("1")).isEqualTo("1");
        assertThat(vocabulary.matches("가입")).isTrue();
        assertThat(vocabulary.matches("가다")).isTrue();
        assertThat(vocabulary.matches("곳")).isTrue();
        assertThat(vocabulary.matches("학기")).isTrue();
        assertThat(vocabulary.matches("가깝다")).isFalse();
        assertThat(vocabulary.matches("단어4")).isFalse();
    }

    @Test
    void stripsHashPrefixedSenseSuffix() {
        assertThat(DetectLabelVocabulary.bare("구형#1")).isEqualTo("구형");
        assertThat(DetectLabelVocabulary.bare("지시#2")).isEqualTo("지시");
        assertThat(DetectLabelVocabulary.bare("다르다#3")).isEqualTo("다르다");
    }

    @Test
    void stripsTrailingLoneHash() {
        assertThat(DetectLabelVocabulary.bare("각각#")).isEqualTo("각각");
        assertThat(DetectLabelVocabulary.bare("각각1#")).isEqualTo("각각");
    }

    @Test
    void allVariantsCollapseToSameBareForm() {
        assertThat(DetectLabelVocabulary.bare("구형")).isEqualTo("구형");
        assertThat(DetectLabelVocabulary.bare("구형1")).isEqualTo("구형");
        assertThat(DetectLabelVocabulary.bare("구형1#")).isEqualTo("구형");
        assertThat(DetectLabelVocabulary.bare("구형#1")).isEqualTo("구형");
        assertThat(DetectLabelVocabulary.bare("구형#")).isEqualTo("구형");
    }

    @Test
    void barePreservesOriginalWhenFullyStripped() {
        assertThat(DetectLabelVocabulary.bare("1")).isEqualTo("1");
        assertThat(DetectLabelVocabulary.bare("#")).isEqualTo("#");
        assertThat(DetectLabelVocabulary.bare("1#")).isEqualTo("1#");
    }

    @Test
    void matchesVariantInputAgainstBareVocabulary() {
        DetectLabelVocabulary bareVocab = new DetectLabelVocabulary(List.of("구형", "지시", "다르다"));
        assertThat(bareVocab.matches("구형#1")).isTrue();
        assertThat(bareVocab.matches("지시2")).isTrue();
        assertThat(bareVocab.matches("다르다3#")).isTrue();
        assertThat(bareVocab.matches("낯선")).isFalse();
    }

    @Test
    void loadsPracticalRecognitionGrades() throws Exception {
        List<String> labels = new ObjectMapper().readValue(
                new ClassPathResource("detect-labels.json").getInputStream(),
                new TypeReference<List<String>>() {
                });
        DetectLabelVocabulary loaded = new DetectLabelVocabulary(new ObjectMapper());

        assertThat(labels).hasSize(251).doesNotHaveDuplicates();
        assertThat(loaded.matches("가능")).isTrue();
        assertThat(loaded.matches("날짜")).isTrue();
        assertThat(loaded.matches("가족")).isTrue();
        assertThat(loaded.matches("시간")).isTrue();
        assertThat(loaded.matches("회")).isTrue();
        assertThat(loaded.matches("가다")).isFalse();
        assertThat(loaded.matches("가깝다")).isFalse();
        assertThat(loaded.matches("간격")).isFalse();
    }
}
