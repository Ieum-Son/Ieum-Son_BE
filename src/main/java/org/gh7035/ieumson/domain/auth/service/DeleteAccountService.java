package org.gh7035.ieumson.domain.auth.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.DeleteAccountRequest;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteAccountService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(CustomUserDetails userDetails,
                        DeleteAccountRequest request) {
        if(!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            throw new IeumException(ErrorCode.PASSWORD_MISMATCH);
        }

        Member member = memberRepository.findById(userDetails.member().getId())
                .orElseThrow(() -> new IeumException(ErrorCode.MEMBER_NOT_FOUND));

        member.leave();
    }
}
