package org.gh7035.ieumson.support;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InMemoryStringRedisTemplate extends StringRedisTemplate {

    private final InMemoryRedisStore store;
    private final ValueOperations<String, String> values;

    @SuppressWarnings("unchecked")
    public InMemoryStringRedisTemplate(InMemoryRedisStore store) {
        setConnectionFactory(mock(RedisConnectionFactory.class));
        this.store = store;
        this.values = mock(ValueOperations.class);
        doAnswer(invocation -> {
            store.set(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
            return null;
        }).when(values).set(anyString(), anyString(), any(Duration.class));
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(invocation -> store.setIfAbsent(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2)));
        when(values.get(anyString())).thenAnswer(invocation -> store.get(invocation.getArgument(0)));
        when(values.getAndDelete(anyString()))
                .thenAnswer(invocation -> store.getAndDelete(invocation.getArgument(0)));
        when(values.increment(anyString()))
                .thenAnswer(invocation -> store.increment(invocation.getArgument(0)));
    }

    @Override
    public ValueOperations<String, String> opsForValue() {
        return values;
    }

    @Override
    public Boolean delete(String key) {
        return store.delete(key);
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return store.expire(key, timeout, unit);
    }

    @Override
    public Long getExpire(String key, TimeUnit timeUnit) {
        return store.getExpire(key, timeUnit);
    }

    @Override
    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        String body = script.getScriptAsString();
        if (body.contains("INCR")) {
            long attempts = store.increment(keys.getFirst());
            if (attempts == 1L) {
                store.expire(keys.getFirst(), Long.parseLong(String.valueOf(args[0])), TimeUnit.SECONDS);
            }
            long maxRequests = Long.parseLong(String.valueOf(args[1]));
            Long allowed = attempts > maxRequests ? 0L : 1L;
            return script.getResultType().cast(allowed);
        }
        throw new UnsupportedOperationException("Unsupported redis script");
    }
}
