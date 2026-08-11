package org.gh7035.ieumson.domain.study.domain;

import jakarta.persistence.*;
import lombok.*;
import org.gh7035.ieumson.global.entity.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Builder(access = AccessLevel.PRIVATE)
@Table(name = "sentence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Sentence extends BaseEntity {

    private static final int OPTION_COUNT = 3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "sentence_kor", nullable = false, length = 200)
    private String sentenceKor; // "안녕하세요, 만나서 반갑습니다"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blank_word_id", nullable = false)
    private Word blankWord; // 빈칸에 들어갈 정답 단어 id

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "json", nullable = false)
    private List<Long> options; // 보기로 보여줄 단어 3개 id (정답 포함)

    @Column(name = "order_num", nullable = false)
    private Integer orderNum;

    public static Sentence of(Lesson lesson, String sentenceKor, Word blankWord, List<Long> options, int orderNum) {
        validateOptions(blankWord, options);
        return Sentence.builder()
                .lesson(lesson)
                .sentenceKor(sentenceKor)
                .blankWord(blankWord)
                .options(List.copyOf(options))
                .orderNum(orderNum)
                .build();
    }

    private static void validateOptions(Word blankWord, List<Long> options) {
        if (options == null || options.size() != OPTION_COUNT) {
            throw new IllegalArgumentException("보기는 " + OPTION_COUNT + "개여야 합니다.");
        }
        if (options.contains(null)) {
            throw new IllegalArgumentException("보기에 비어 있는 값이 있습니다.");
        }
        if (options.stream().distinct().count() != OPTION_COUNT) {
            throw new IllegalArgumentException("보기에 중복된 단어가 있습니다: " + options);
        }
        Long answerId = blankWord.getId();
        if (answerId == null) {
            throw new IllegalArgumentException("정답 단어가 저장되지 않아 보기를 구성할 수 없습니다.");
        }
        if (!options.contains(answerId)) {
            throw new IllegalArgumentException("보기에 정답 단어가 포함되어야 합니다: " + answerId);
        }
    }
}
