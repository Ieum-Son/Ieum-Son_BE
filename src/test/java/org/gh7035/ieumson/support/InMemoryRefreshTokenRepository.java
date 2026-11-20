package org.gh7035.ieumson.support;

import org.gh7035.ieumson.domain.auth.domain.RefreshToken;
import org.gh7035.ieumson.domain.auth.domain.repository.RefreshTokenRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final ConcurrentHashMap<String, RefreshToken> tokens = new ConcurrentHashMap<>();

    @Override
    public <S extends RefreshToken> S save(S entity) {
        tokens.put(entity.getLoginId(), copy(entity));
        return entity;
    }

    @Override
    public <S extends RefreshToken> Iterable<S> saveAll(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();
        for (S entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }

    @Override
    public Optional<RefreshToken> findById(String loginId) {
        return Optional.ofNullable(tokens.get(loginId)).map(this::copy);
    }

    @Override
    public boolean existsById(String loginId) {
        return tokens.containsKey(loginId);
    }

    @Override
    public Iterable<RefreshToken> findAll() {
        return tokens.values().stream().map(this::copy).toList();
    }

    @Override
    public Iterable<RefreshToken> findAllById(Iterable<String> loginIds) {
        List<RefreshToken> found = new ArrayList<>();
        for (String loginId : loginIds) {
            findById(loginId).ifPresent(found::add);
        }
        return found;
    }

    @Override
    public long count() {
        return tokens.size();
    }

    @Override
    public void deleteById(String loginId) {
        tokens.remove(loginId);
    }

    @Override
    public void delete(RefreshToken entity) {
        tokens.remove(entity.getLoginId());
    }

    @Override
    public void deleteAllById(Iterable<? extends String> loginIds) {
        for (String loginId : loginIds) {
            tokens.remove(loginId);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends RefreshToken> entities) {
        for (RefreshToken entity : entities) {
            tokens.remove(entity.getLoginId());
        }
    }

    @Override
    public void deleteAll() {
        tokens.clear();
    }

    @Override
    public int rotateIfMatches(String loginId, String expectedToken, String newToken, long ttlMs) {
        RefreshToken current = tokens.get(loginId);
        if (current == null) {
            return -1;
        }
        if (current.getRefreshToken() == null || !current.getRefreshToken().equals(expectedToken)) {
            return 0;
        }
        tokens.put(loginId, RefreshToken.builder()
                .loginId(loginId)
                .refreshToken(newToken)
                .expireTime(ttlMs)
                .build());
        return 1;
    }

    public void clear() {
        tokens.clear();
    }

    private RefreshToken copy(RefreshToken token) {
        return RefreshToken.builder()
                .loginId(token.getLoginId())
                .refreshToken(token.getRefreshToken())
                .expireTime(token.getExpireTime())
                .build();
    }
}
