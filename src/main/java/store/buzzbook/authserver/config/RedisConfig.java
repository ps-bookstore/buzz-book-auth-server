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

        String keyManagerUrl = "https://api-keymanager.nhncloudservice.com/keymanager/v1.0/appkey/%s/secrets/%s";

        SecretResponse database = restTemplate.getForObject(
                String.format(keyManagerUrl, appKey, redisDatabase), SecretResponse.class);

        SecretResponse host = restTemplate.getForObject(
                String.format(keyManagerUrl, appKey, redisHost), SecretResponse.class);

        SecretResponse port = restTemplate.getForObject(
                String.format(keyManagerUrl, appKey, redisPort), SecretResponse.class);

        SecretResponse password = restTemplate.getForObject(
                String.format(keyManagerUrl, appKey, redisPassword), SecretResponse.class);


        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(Objects.requireNonNull(host).getBody().getSecret());
        redisStandaloneConfiguration.setPort(Integer.parseInt(Objects.requireNonNull(port).getBody().getSecret()));
        redisStandaloneConfiguration.setPassword(Objects.requireNonNull(password).getBody().getSecret());
        redisStandaloneConfiguration.setDatabase(Integer.parseInt(Objects.requireNonNull(database).getBody().getSecret()));
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

}
