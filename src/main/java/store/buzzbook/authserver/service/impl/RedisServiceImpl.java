package store.buzzbook.authserver.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import store.buzzbook.authserver.client.DoorayClient;
import store.buzzbook.authserver.dto.DoorayMessagePayload;
import store.buzzbook.authserver.exception.ActivateFailException;
import store.buzzbook.authserver.exception.DoorayException;
import store.buzzbook.authserver.service.RedisService;
import store.buzzbook.authserver.util.AuthCodeGenerator;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {
    private final RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, Object> hashOperations;
    private final DoorayClient doorayClient;


    @PostConstruct
    void init() {
        this.hashOperations = redisTemplate.opsForHash();
    }

    @Override
    public void saveUser(String userId, Map<String, Object> data) {
        hashOperations.putAll(userId, data);

        long userTTL = 7;

        redisTemplate.expire(userId, userTTL, TimeUnit.DAYS);
    }

    @Override
    public Map<String, Object> getUser(String userId) {
        return hashOperations.entries(userId);
    }

    @Override
    public void removeUser(String uuid) {
        try {
            redisTemplate.delete(uuid);
        } catch (Exception e) {
            log.error("레디스에서 유저 정보 삭제 실패", e);
        }

    }

    @Override
    public String createDormantToken(String loginId) {
        UUID uuid = UUID.randomUUID();
        String code = AuthCodeGenerator.generate();

        String hashKey = String.format("%s%s", DORMANT_HASH_PREFIX, uuid);
        redisTemplate.opsForHash().put(hashKey, DORMANT_LOGIN_ID_KEY, loginId);
        redisTemplate.opsForHash().put(hashKey, DORMANT_CODE_KEY, code);
        redisTemplate.expire(hashKey, DEFAULT_EXPIRATION, TimeUnit.SECONDS);

        DoorayMessagePayload messagePayload = DoorayMessagePayload.builder()
                .text(code)
                .botName(AUTH_BOT_NAME)
                .botIconImage("/static/images/buzz bee.png")
                .build();

        ResponseEntity<String> responseEntity = doorayClient.sendMessage(messagePayload);

        if (responseEntity.getStatusCode().isError()) {
            throw new DoorayException();
        }

        return hashKey;
    }

    @Override
    public boolean isDormantToken(String token) {
        return redisTemplate.opsForHash().hasKey(token, DORMANT_CODE_KEY) && redisTemplate.opsForHash().hasKey(token, DORMANT_LOGIN_ID_KEY);
    }

    @Override
    public String checkDormantToken(String token, String code) {
        String expectCode = (String) redisTemplate.opsForHash().get(token, DORMANT_CODE_KEY);

        if (!Objects.requireNonNull(expectCode).equals(code)) {
            log.debug("코드 인증에 실패했습니다.");
            throw new ActivateFailException();
        }

        String loginId = (String) redisTemplate.opsForHash().get(token, DORMANT_LOGIN_ID_KEY);

        if (Objects.isNull(loginId)) {
            log.debug("로그인 아이디가 발견되지 않았습니다.");
            throw new ActivateFailException("이미 인증 되었거나 타임아웃이 발생했습니다.");
        }

        redisTemplate.delete(token);

        return loginId;
    }

}
