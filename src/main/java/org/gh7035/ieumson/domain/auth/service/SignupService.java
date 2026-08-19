package org.gh7035.ieumson.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.SignUpRequest;
import org.gh7035.ieumson.domain.auth.presentation.dto.response.ProfileImageResponse;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.enums.Role;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.infrastructure.s3.S3FileUploader;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class SignupService {

    private final MemberRepository memberRepository;
    private final MemberValidator memberValidator;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final S3FileUploader s3FileUploader;

    private static final String VERIFIED_KEY_PREFIX = "auth:signup:verified:";

    public ProfileImageResponse execute(SignUpRequest request, MultipartFile image) {
        memberValidator.validateEmailNotExists(request.email());
        memberValidator.validateLoginIdNotExists(request.loginId());

        String verifiedKey = VERIFIED_KEY_PREFIX + request.email();
        String verified = stringRedisTemplate.opsForValue().get(verifiedKey);
        if (verified == null) {
            throw new IeumException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        String profileImageUrl = s3FileUploader.uploadProfileImage(request.loginId(), image);

        try {
            memberRepository.saveAndFlush(Member.builder()
                    .email(request.email())
                    .name(request.name())
                    .loginId(request.loginId())
                    .password(passwordEncoder.encode(request.password()))
                    .profileImageUrl(profileImageUrl)
                    .emailVerified(true)
                    .role(Role.USER)
                    .build());
        } catch (DataIntegrityViolationException e) {
            s3FileUploader.deleteIfOwned(profileImageUrl);
            String message = String.valueOf(e.getMostSpecificCause().getMessage())
                    .toLowerCase(Locale.ROOT);
            if (message.contains("login_id")) {
                throw new IeumException(ErrorCode.LOGIN_ID_ALREADY_EXISTS, e);
            }
            if (message.contains("email")) {
                throw new IeumException(ErrorCode.EMAIL_ALREADY_EXISTS, e);
            }
            throw new IeumException(ErrorCode.DUPLICATE_ENTRY, e);
        } catch (RuntimeException e) {
            s3FileUploader.deleteIfOwned(profileImageUrl);
            throw e;
        }

        deleteVerifiedKeyAfterCommit(verifiedKey);
        return new ProfileImageResponse(profileImageUrl);
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
