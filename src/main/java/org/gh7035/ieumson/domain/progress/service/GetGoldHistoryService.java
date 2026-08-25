package org.gh7035.ieumson.domain.progress.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.GoldTransaction;
import org.gh7035.ieumson.domain.progress.domain.repository.GoldTransactionRepository;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.GoldHistoryResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.GoldHistoryResponse.GoldTransactionResponse;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetGoldHistoryService {

    private final CurrentMemberFinder currentMemberFinder;
    private final GoldTransactionRepository goldTransactionRepository;

    public GoldHistoryResponse execute(CustomUserDetails userDetails, Pageable pageable) {
        Member member = currentMemberFinder.get(userDetails);
        Page<GoldTransaction> page = goldTransactionRepository.findByMemberOrderByCreatedAtDesc(member, pageable);
        return new GoldHistoryResponse(
                member.getNuggetBalance(),
                page.getContent().stream()
                        .map(tx -> new GoldTransactionResponse(
                                tx.getId(),
                                tx.getAmount(),
                                tx.getType(),
                                tx.getDescription(),
                                tx.getCreatedAt()
                        ))
                        .toList()
        );
    }
}
