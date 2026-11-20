package org.gh7035.ieumson.domain.auth.domain.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepositoryCustom {

    private static final String KEY_PREFIX = "refresh_token:";

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            if redis.call('EXISTS', key) == 0 then
                return -1
            end
            local current = redis.call('HGET', key, 'refreshToken')
            if not current or current ~= ARGV[1] then
                return 0
            end
            redis.call('HSET', key, 'refreshToken', ARGV[2], 'expireTime', ARGV[3])
            redis.call('PEXPIRE', key, ARGV[3])
            return 1
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public int rotateIfMatches(String loginId, String expectedToken, String newToken, long ttlMs) {
        Long result = stringRedisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(KEY_PREFIX + loginId),
                expectedToken,
                newToken,
                String.valueOf(ttlMs)
        );
        return result != null ? result.intValue() : 0;
    }
}
