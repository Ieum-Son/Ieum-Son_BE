package org.gh7035.ieumson.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentMemberFinder {

    private final MemberRepository memberRepository;

    public Member get(CustomUserDetails userDetails) {
        return memberRepository.findById(userDetails.member().getId())
                .orElseThrow(() -> new IeumException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
