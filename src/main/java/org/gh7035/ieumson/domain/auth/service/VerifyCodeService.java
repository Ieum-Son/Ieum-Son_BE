package org.gh7035.ieumson.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.VerifyCodeRequest;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class VerifyCodeService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String CODE_KEY_PREFIX = "auth:signup:code:";
    private static final String VERIFIED_KEY_PREFIX = "auth:signup:verified:";

    @Value("${app.signup.verified-expiration-minutes:30}")
    private long verifiedExpirationMinutes;

    public void verifyCode(VerifyCodeRequest request) {
        String codeKey = CODE_KEY_PREFIX + request.email();
        String storedCode = stringRedisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            throw new IeumException(ErrorCode.VERIFY_CODE_EXPIRED);
        }
        if (!storedCode.equals(request.code())) {
            throw new IeumException(ErrorCode.VERIFY_CODE_MISMATCH);
        }

        stringRedisTemplate.delete(codeKey);
        stringRedisTemplate.opsForValue().set(
                VERIFIED_KEY_PREFIX + request.email(),
                "true",
                Duration.ofMinutes(verifiedExpirationMinutes)
        );
    }
}
