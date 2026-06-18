package org.gh7035.ieumson.domain.auth.domain;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@Builder
@RedisHash // RDB가 아닌 Redis에 저장하겠다
public class RefreshToken {
    @Id
    private String email;

    @Indexed
    private String refreshToken;

    @TimeToLive
    private Long expireTime;

    public void rotationToken(String refreshToken, Long expireTime) {
        this.refreshToken = refreshToken;
        this.expireTime = expireTime;
    }
}
