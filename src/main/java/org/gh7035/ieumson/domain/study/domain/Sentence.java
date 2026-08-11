package org.gh7035.ieumson.domain.study.domain;

import jakarta.persistence.*;
import lombok.*;
import org.gh7035.ieumson.global.entity.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Builder
@Table(name = "sentence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Sentence extends BaseEntity {

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
}
