package org.gh7035.ieumson.domain.word.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.gh7035.ieumson.global.entity.BaseEntity;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "word")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Word extends BaseEntity {
    private String word;

    private String definition;

    private String videoLink;

    private String category;

    private int difficulty = 1;

    
}
