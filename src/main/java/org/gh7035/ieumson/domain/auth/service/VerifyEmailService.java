package org.gh7035.ieumson.domain.auth.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.VerifyRequest;
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
    private final MemberValidator memberValidator;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String SIGNUP_CODE_KEY_PREFIX = "auth:signup:code:";
    private static final String SIGNUP_COOLDOWN_KEY_PREFIX = "auth:signup:verify:cooldown:";
    private static final String SIGNUP_VERIFY_RATE_IP_KEY_PREFIX = "auth:signup:verify:rate:ip:";
    private static final String SIGNUP_VERIFY_RATE_EMAIL_KEY_PREFIX = "auth:signup:verify:rate:email:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.signup.code-expiration-minutes:10}")
    private long signupCodeExpirationMinutes;

    @Value("${app.signup.verify-resend-cooldown-seconds:60}")
    private long verifyResendCooldownSeconds;

    @Value("${app.signup.verify-max-requests-per-ip:20}")
    private long verifyMaxRequestsPerIp;

    @Value("${app.signup.verify-max-requests-per-email:10}")
    private long verifyMaxRequestsPerEmail;

    @Value("${app.signup.verify-rate-limit-window-seconds:300}")
    private long verifyRateLimitWindowSeconds;

    @Transactional
    public void verifyEmail(VerifyRequest request, String clientIp) {
        memberValidator.validateEmailNotExists(request.email());
        applyRateLimit(signupVerifyRateEmailKey(request.email()), verifyMaxRequestsPerEmail, verifyRateLimitWindowSeconds);
        if (clientIp != null && !clientIp.isBlank()) {
            applyRateLimit(signupVerifyRateIpKey(clientIp), verifyMaxRequestsPerIp, verifyRateLimitWindowSeconds);
        }

        Boolean cooldownSet = stringRedisTemplate.opsForValue().setIfAbsent(
                signupCooldownKey(request.email()),
                "1",
                Duration.ofSeconds(verifyResendCooldownSeconds)
        );
        if (Boolean.FALSE.equals(cooldownSet)) {
            throw new IeumException(ErrorCode.VERIFY_EMAIL_RESEND_TOO_FAST);
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
            stringRedisTemplate.delete(signupCooldownKey(request.email()));
            throw new IeumException(ErrorCode.AUTH_MAIL_SEND_FAILED, e);
        }
    }

    private String generateSignupCode() {
        int value = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private String signupCodeKey(String email) {
        return SIGNUP_CODE_KEY_PREFIX + email;
    }

    private String signupCooldownKey(String email) {
        return SIGNUP_COOLDOWN_KEY_PREFIX + email;
    }

    private String signupVerifyRateIpKey(String clientIp) {
        return SIGNUP_VERIFY_RATE_IP_KEY_PREFIX + clientIp;
    }

    private String signupVerifyRateEmailKey(String email) {
        return SIGNUP_VERIFY_RATE_EMAIL_KEY_PREFIX + email;
    }

    private void applyRateLimit(String key, long maxRequests, long windowSeconds) {
        if (maxRequests <= 0 || windowSeconds <= 0) {
            return;
        }

        Long attempts = stringRedisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        if (attempts != null && attempts > maxRequests) {
            throw new IeumException(ErrorCode.AUTH_REQUEST_RATE_LIMITED);
        }
    }
}