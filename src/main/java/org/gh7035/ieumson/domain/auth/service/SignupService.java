package org.gh7035.ieumson.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.SignUpRequest;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.enums.Role;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SignupService {

    private final MemberRepository memberRepository;
    private final MemberValidator memberValidator;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String VERIFIED_KEY_PREFIX = "auth:signup:verified:";

    public void signUp(SignUpRequest request) {
        memberValidator.validateEmailNotExists(request.email());

        String verifiedKey = VERIFIED_KEY_PREFIX + request.email();
        String verified = stringRedisTemplate.opsForValue().get(verifiedKey);

        if (verified == null) {
            throw new IeumException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        try {
            memberRepository.save(Member.builder()
                    .email(request.email())
                    .nickname(request.nickname())
                    .password(passwordEncoder.encode(request.password()))
                    .emailVerified(true)
                    .role(Role.USER)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new IeumException(ErrorCode.EMAIL_ALREADY_EXISTS, e);
        }

        stringRedisTemplate.delete(verifiedKey);
    }
}
