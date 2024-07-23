package store.buzzbook.authserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

import store.buzzbook.authserver.dto.SecretResponse;

import java.util.Objects;

@Configuration
@EnableRedisRepositories
public class RedisConfig {

    private static final String KEY_MANAGER_URL_PATTERN = "https://api-keymanager.nhncloudservice.com/keymanager/v1.0/appkey/%s/secrets/%s";

    @Value("${nhncloud.keymanager.appkey}")
    private String appKey;

    @Value("${nhncloud.keymanager.redis.database}")
    private String redisDatabase;

    @Value("${nhncloud.keymanager.redis.host}")
    private String redisHost;

    @Value("${nhncloud.keymanager.redis.port}")
    private String redisPort;

    @Value("${nhncloud.keymanager.redis.password}")
    private String redisPassword;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RestTemplate restTemplate = new RestTemplate();

        String databaseSecret = fetchSecret(restTemplate, redisDatabase);
        String hostSecret = fetchSecret(restTemplate, redisHost);
        String portSecret = fetchSecret(restTemplate, redisPort);
        String passwordSecret = fetchSecret(restTemplate, redisPassword);

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(hostSecret);
        redisStandaloneConfiguration.setPort(Integer.parseInt(portSecret));
        redisStandaloneConfiguration.setPassword(passwordSecret);
        redisStandaloneConfiguration.setDatabase(Integer.parseInt(databaseSecret));

        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    protected String fetchSecret(RestTemplate restTemplate, String secretKey) {
        SecretResponse response = restTemplate.getForObject(
                String.format(KEY_MANAGER_URL_PATTERN, appKey, secretKey), SecretResponse.class);
        return Objects.requireNonNull(response).getBody().getSecret();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        setSerializers(redisTemplate);
        return redisTemplate;
    }

    public void setSerializers(RedisTemplate<String, Object> redisTemplate) {
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
    }
}
