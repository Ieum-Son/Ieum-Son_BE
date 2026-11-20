package org.gh7035.ieumson.domain.progress.service.ranking;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.RankingResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.RankingResponse.RankingItemResponse;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRankingService {

    private final CurrentMemberFinder currentMemberFinder;
    private final MemberRepository memberRepository;

    public RankingResponse execute(CustomUserDetails userDetails, Pageable pageable) {
        Member me = currentMemberFinder.get(userDetails);
        Page<Member> page = memberRepository.findAllByOrderByNuggetBalanceDescIdAsc(pageable);
        int myRank = (int) memberRepository.countByNuggetBalanceGreaterThan(me.getNuggetBalance()) + 1;

        int startRank = (int) pageable.getOffset() + 1;
        List<RankingItemResponse> items = new ArrayList<>();
        int rank = startRank;
        for (Member member : page.getContent()) {
            items.add(new RankingItemResponse(
                    rank++,
                    member.getName(),
                    member.getProfileImageUrl(),
                    member.getNuggetBalance(),
                    member.getId().equals(me.getId())
            ));
        }

        return new RankingResponse(myRank, me.getNuggetBalance(), page.getTotalElements(), items);
    }
}
