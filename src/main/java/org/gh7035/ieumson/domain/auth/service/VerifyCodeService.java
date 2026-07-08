package org.gh7035.ieumson.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.VerifyCodeRequest;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VerifyCodeService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String CODE_KEY_PREFIX = "auth:signup:code:";
    private static final String VERIFIED_KEY_PREFIX = "auth:signup:verified:";
    private static final String CODE_FAILURE_KEY_PREFIX = "auth:signup:code:fail:";
    private static final String CODE_RATE_IP_KEY_PREFIX = "auth:signup:code:rate:ip:";
    private static final String CODE_RATE_EMAIL_KEY_PREFIX = "auth:signup:code:rate:email:";

    @Value("${app.signup.verified-expiration-minutes:30}")
    private long verifiedExpirationMinutes;

    @Value("${app.signup.code-expiration-minutes:10}")
    private long codeExpirationMinutes;

    @Value("${app.signup.code-max-failures:5}")
    private long codeMaxFailures;

    @Value("${app.signup.code-max-requests-per-ip:30}")
    private long codeMaxRequestsPerIp;

    @Value("${app.signup.code-max-requests-per-email:15}")
    private long codeMaxRequestsPerEmail;

    @Value("${app.signup.code-rate-limit-window-seconds:300}")
    private long codeRateLimitWindowSeconds;

    public void verifyCode(VerifyCodeRequest request) {
        verifyCode(request, null);
    }

    public void verifyCode(VerifyCodeRequest request, String clientIp) {
        String codeKey = CODE_KEY_PREFIX + request.email();
        String failureKey = CODE_FAILURE_KEY_PREFIX + request.email();

        applyRateLimit(codeRateEmailKey(request.email()), codeMaxRequestsPerEmail, codeRateLimitWindowSeconds);
        if (clientIp != null && !clientIp.isBlank()) {
            applyRateLimit(codeRateIpKey(clientIp), codeMaxRequestsPerIp, codeRateLimitWindowSeconds);
        }

        Long codeTtlSeconds = stringRedisTemplate.getExpire(codeKey, TimeUnit.SECONDS);
        String storedCode = stringRedisTemplate.opsForValue().getAndDelete(codeKey);

        if (storedCode == null) {
            throw new IeumException(ErrorCode.VERIFY_CODE_EXPIRED);
        }
        if (!storedCode.equals(request.code())) {
            Long failures = stringRedisTemplate.opsForValue().increment(failureKey);

            if (failures != null && failures == 1L) {
                if (codeTtlSeconds != null && codeTtlSeconds > 0) {
                    stringRedisTemplate.expire(failureKey, Duration.ofSeconds(codeTtlSeconds));
                } else {
                    stringRedisTemplate.expire(failureKey, Duration.ofMinutes(codeExpirationMinutes));
                }
            }

            if (failures != null && failures >= codeMaxFailures) {
                stringRedisTemplate.delete(failureKey);
                throw new IeumException(ErrorCode.VERIFY_CODE_EXPIRED);
            }

            if (codeTtlSeconds != null && codeTtlSeconds > 0) {
                stringRedisTemplate.opsForValue().set(codeKey, storedCode, Duration.ofSeconds(codeTtlSeconds));
            } else {
                stringRedisTemplate.opsForValue().set(codeKey, storedCode, Duration.ofMinutes(codeExpirationMinutes));
            }

            throw new IeumException(ErrorCode.VERIFY_CODE_MISMATCH);
        }

        stringRedisTemplate.delete(failureKey);
        stringRedisTemplate.opsForValue().set(
                VERIFIED_KEY_PREFIX + request.email(),
                "true",
                Duration.ofMinutes(verifiedExpirationMinutes)
        );
    }

    private String codeRateIpKey(String clientIp) {
        return CODE_RATE_IP_KEY_PREFIX + clientIp;
    }

    private String codeRateEmailKey(String email) {
        return CODE_RATE_EMAIL_KEY_PREFIX + email;
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
