package org.gh7035.ieumson.domain.progress.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GoldTransactionType {
    EARN_LEARNING(true),
    EARN_STREAK_BONUS(true),
    SPEND_STREAK_RECOVERY(false),
    BUY_ITEM(false);

    private final boolean earning;
}
