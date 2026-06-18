package org.gh7035.ieumson.global.security.jwt;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Base64;

@Getter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private final String header; // jwt 토큰의 헤더명
    private final String prefix; // 토큰 앞에 붙을 접두사
    private final String secretKey; // 서명 키
    private final Long accessTokenExpiry; // 액세스 토큰 만료 (ms)
    private final Long refreshTokenExpiry; // 리프레시 토큰 만료 (ms)

    public JwtProperties(String header,
                         String prefix,
                         String secretKey,
                         Long accessTokenExpiry,
                         Long refreshTokenExpiry) {
        this.header = header;
        this.prefix = prefix;
        this.secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }
}