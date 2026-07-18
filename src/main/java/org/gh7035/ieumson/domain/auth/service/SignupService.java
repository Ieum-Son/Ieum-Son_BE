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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class SignupService {

    private final MemberRepository memberRepository;
    private final MemberValidator memberValidator;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String VERIFIED_KEY_PREFIX = "auth:signup:verified:";

    public void execute(SignUpRequest request) {
        memberValidator.validateEmailNotExists(request.email());
        memberValidator.validateLoginIdNotExists(request.loginId());

        String verifiedKey = VERIFIED_KEY_PREFIX + request.email();
        String verified = stringRedisTemplate.opsForValue().get(verifiedKey);

        if (verified == null) {
            throw new IeumException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        try {
            memberRepository.saveAndFlush(Member.builder()
                    .email(request.email())
                    .name(request.name())
                    .loginId(request.loginId())
                    .password(passwordEncoder.encode(request.password()))
                    .emailVerified(true)
                    .role(Role.USER)
                    .build());
        } catch (DataIntegrityViolationException e) {
            String message = String.valueOf(e.getMostSpecificCause().getMessage())
                    .toLowerCase(Locale.ROOT);
            if (message.contains("login_id")) {
                throw new IeumException(ErrorCode.LOGIN_ID_ALREADY_EXISTS, e);
            }
            if (message.contains("email")) {
                throw new IeumException(ErrorCode.EMAIL_ALREADY_EXISTS, e);
            }
            throw new IeumException(ErrorCode.DUPLICATE_ENTRY, e);
        }

        deleteVerifiedKeyAfterCommit(verifiedKey);
    }

    private void deleteVerifiedKeyAfterCommit(String verifiedKey) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    stringRedisTemplate.delete(verifiedKey);
                }
            });
            return;
        }

        stringRedisTemplate.delete(verifiedKey);
    }
}
