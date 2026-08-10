package org.gh7035.ieumson.domain.progress.domain;

import jakarta.persistence.*;
import lombok.*;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.progress.domain.enums.GoldTransactionType;
import org.gh7035.ieumson.global.entity.BaseEntity;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "gold_transaction")
public class GoldTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "amount", nullable = false)
    private Integer amount; // +6 (획득), -35 (스트릭 회복)

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private GoldTransactionType type; // EARN_LEARNING, SPEND_STREAK_RECOVERY 등

    @Column(name = "description")
    private String description; // "8/10 학습 완료", "스트릭 회복"
}