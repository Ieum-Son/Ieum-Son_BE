package org.gh7035.ieumson.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local attempts = redis.call('INCR', KEYS[1])
            if attempts == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            if attempts > tonumber(ARGV[2]) then
                return 0
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    public void check(String key, long maxRequests, long windowSeconds) {
        if (maxRequests <= 0 || windowSeconds <= 0) {
            return;
        }

        Long allowed = stringRedisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(windowSeconds),
                String.valueOf(maxRequests)
        );

        if (allowed == null) {
            throw new IllegalStateException("Redis rate limit script returned no result");
        }
        if (allowed == 0L) {
            throw new IeumException(ErrorCode.AUTH_REQUEST_RATE_LIMITED);
        }
    }
}
