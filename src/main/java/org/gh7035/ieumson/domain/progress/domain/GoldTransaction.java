package org.gh7035.ieumson.domain.progress.domain;

import jakarta.persistence.*;
import lombok.*;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.progress.domain.enums.GoldTransactionType;
import org.gh7035.ieumson.global.entity.BaseEntity;

@Entity
@Builder(access = AccessLevel.PRIVATE)
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

    /**
     * amount는 항상 양수로 전달하고, 획득/사용에 따른 부호는 거래 유형이 결정한다.
     */
    public static GoldTransaction of(Member member, GoldTransactionType type, int amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("거래 금액은 1 이상이어야 합니다: " + amount);
        }
        return GoldTransaction.builder()
                .member(member)
                .type(type)
                .amount(type.isEarning() ? amount : -amount)
                .description(description)
                .build();
    }
}