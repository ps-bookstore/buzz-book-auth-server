// package store.buzzbook.authserver.config;
//
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.lenient;
//
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
// import org.springframework.data.redis.serializer.StringRedisSerializer;
// import org.springframework.web.client.RestTemplate;
//
// import static org.junit.jupiter.api.Assertions.*;
//
// @ExtendWith(MockitoExtension.class)
// class RedisConfigTest {
//
//     @Mock
//     private RestTemplate restTemplate;
//
//     @InjectMocks
//     private RedisConfig redisConfig;
//
//     @BeforeEach
//     void setUp() {
//         Body body = new Body();
//         body.setSecret("dummy-secret");
//
//         SecretResponse secretResponse = new SecretResponse();
//         secretResponse.setBody(body);
//
//         lenient().when(restTemplate.getForObject(anyString(), any(Class.class)))
//                 .thenReturn(secretResponse);
//     }
//
//     @Test
//     void testFetchSecret() {
//         String secret = redisConfig.fetchSecret(restTemplate, "dummy-key");
//         assertEquals("dummy-secret", secret);
//     }
//
//     @Test
//     void testSetSerializers() {
//         RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//         redisConfig.setSerializers(redisTemplate);
//
//         assertInstanceOf(StringRedisSerializer.class, redisTemplate.getKeySerializer());
//         assertInstanceOf(StringRedisSerializer.class, redisTemplate.getValueSerializer());
//         assertInstanceOf(StringRedisSerializer.class, redisTemplate.getHashKeySerializer());
//         assertInstanceOf(GenericJackson2JsonRedisSerializer.class, redisTemplate.getHashValueSerializer());
//     }
// }
