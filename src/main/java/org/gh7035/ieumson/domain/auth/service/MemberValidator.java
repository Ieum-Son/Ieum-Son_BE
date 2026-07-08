package org.gh7035.ieumson.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberValidator {

    private final MemberRepository memberRepository;

    public void validateEmailNotExists(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new IeumException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }
}
