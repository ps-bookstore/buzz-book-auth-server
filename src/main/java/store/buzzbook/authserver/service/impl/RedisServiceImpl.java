package store.buzzbook.authserver.service.impl;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import store.buzzbook.authserver.service.RedisService;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RedisServiceImpl implements RedisService {
	private final RedisTemplate<String, Object> redisTemplate;
	private HashOperations<String, String, Object> hashOperations;

	@Autowired
	public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@PostConstruct
	private void init() {
		this.hashOperations = redisTemplate.opsForHash();
	}

	public void setKey(String key, String value) {
		ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
		valueOperations.set(key, value);
	}

	public String getKey(String key) {
		ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
		return (String)valueOperations.get(key);
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
		redisTemplate.delete(uuid);
	}
}
