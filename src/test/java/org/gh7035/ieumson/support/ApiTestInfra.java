package org.gh7035.ieumson.support;

import org.gh7035.ieumson.domain.auth.domain.repository.RefreshTokenRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("api-test")
public class ApiTestInfra {

    @Bean
    InMemoryRedisStore inMemoryRedisStore() {
        return new InMemoryRedisStore();
    }

    @Bean
    @Primary
    RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    StringRedisTemplate stringRedisTemplate(InMemoryRedisStore store) {
        return new InMemoryStringRedisTemplate(store);
    }

    @Bean
    @Primary
    RefreshTokenRepository inMemoryRefreshTokenRepository() {
        return new InMemoryRefreshTokenRepository();
    }
}
