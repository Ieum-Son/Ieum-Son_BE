package org.gh7035.ieumson.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.VerifyRequest;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.infrastructure.mail.MailSenderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VerifyEmailService {

    private final MailSenderService mailSender;
    private final MemberRepository memberRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String SIGNUP_CODE_KEY_PREFIX = "auth:signup:code:";

    @Value("${app.signup.code-expiration-minutes:10}")
    private long signupCodeExpirationMinutes;

    public void verifyEmail(VerifyRequest request) {
        if (memberRepository.findByEmail(request.email()).isPresent()) {
            throw new IeumException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        String code = generateSignupCode();
        stringRedisTemplate.opsForValue().set(
                signupCodeKey(request.email()),
                code,
                Duration.ofMinutes(signupCodeExpirationMinutes)
        );
        try {
            mailSender.sendHtmlTemplate(
                    request.email(),
                    "[이음손] 회원가입 인증코드",
                    "templates/mailTemplate.html",
                    Map.of("code", code)
            );
        } catch (MailException | IllegalStateException e) {
            throw new IeumException(ErrorCode.AUTH_MAIL_SEND_FAILED);
        }
    }

    private String generateSignupCode() {
        SecureRandom random = new SecureRandom();
        int value = random.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private String signupCodeKey(String email) {
        return SIGNUP_CODE_KEY_PREFIX + email;
    }
}
