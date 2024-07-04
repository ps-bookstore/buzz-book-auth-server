package store.buzzbook.authserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

import store.buzzbook.authserver.dto.SecretResponse;

@Configuration
@EnableRedisRepositories
@Profile("prod")
public class RedisProdConfig {

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

		SecretResponse database = restTemplate.getForObject(
			String.format("https://api-keymanager.nhncloudservice.com/keymanager/v1.0/appkey/%s/secrets/%s", appKey, redisDatabase), SecretResponse.class);

		SecretResponse host = restTemplate.getForObject(
			String.format("https://api-keymanager.nhncloudservice.com/keymanager/v1.0/appkey/%s/secrets/%s", appKey, redisHost), SecretResponse.class);

		SecretResponse port = restTemplate.getForObject(
			String.format("https://api-keymanager.nhncloudservice.com/keymanager/v1.0/appkey/%s/secrets/%s", appKey, redisPort), SecretResponse.class);

		SecretResponse password = restTemplate.getForObject(
			String.format("https://api-keymanager.nhncloudservice.com/keymanager/v1.0/appkey/%s/secrets/%s", appKey, redisPassword), SecretResponse.class);


		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
		redisStandaloneConfiguration.setHostName(host.getBody().getSecret());
		redisStandaloneConfiguration.setPort(Integer.parseInt(port.getBody().getSecret()));
		redisStandaloneConfiguration.setPassword(password.getBody().getSecret());
		redisStandaloneConfiguration.setDatabase(Integer.parseInt(database.getBody().getSecret()));
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
