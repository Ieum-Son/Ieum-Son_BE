package org.gh7035.ieumson.domain.progress.presentation.dto.response;

import org.gh7035.ieumson.domain.progress.domain.enums.GoldTransactionType;

import java.time.LocalDateTime;
import java.util.List;

public record GoldHistoryResponse(
        int balance,
        List<GoldTransactionResponse> items
) {
    public record GoldTransactionResponse(
            Long id,
            int amount,
            GoldTransactionType type,
            String description,
            LocalDateTime createdAt
    ) {
    }
}
