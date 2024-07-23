package store.buzzbook.authserver.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import store.buzzbook.authserver.client.DoorayClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private DoorayClient doorayClient;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisServiceImpl redisService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisService.init(); // @PostConstruct 메서드 수동 호출
    }

    @Test
    void testSetKey() {
        redisService.setKey("testKey", "testValue");
        verify(valueOperations).set("testKey", "testValue");
    }

    @Test
    void testGetKey() {
        when(valueOperations.get("testKey")).thenReturn("testValue");
        String value = redisService.getKey("testKey");
        assertEquals("testValue", value);
    }

    @Test
    void testSaveUser() {
        Map<String, Object> data = new HashMap<>();
        UUID uuid = UUID.randomUUID();
        data.put("userId", 123);
        data.put("loginId", "12345");
        data.put("role", "USER");

        redisService.saveUser(uuid.toString(), data);
        verify(hashOperations).putAll(uuid.toString(), data);
        verify(redisTemplate).expire(uuid.toString(), 7, TimeUnit.DAYS);
    }

    @Test
    void testGetUser() {
        Map<Object, Object> data = new HashMap<>();
        data.put("key", "value");
        when(hashOperations.entries("userId")).thenReturn(data);
        Map<String, Object> result = redisService.getUser("userId");
        assertEquals(data, result);
    }

    @Test
    void testRemoveUser() {
        redisService.removeUser("userId");
        verify(redisTemplate).delete("userId");
    }

}
