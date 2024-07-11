package store.buzzbook.authserver.service;


import java.util.Map;

public interface RedisService {
	String DORMANT_HASH_PREFIX = "DH_";
	String DORMANT_LOGIN_ID_KEY = "loginId";
	String DORMANT_CODE_KEY = "code";
	Long DEFAULT_EXPIRATION = 180L;
	String AUTH_BOT_NAME = "Buzz-Bee";


	void setKey(String key, String value);
	String getKey(String key);
	void saveUser(String userId, Map<String, Object> data);
	Map<String, Object> getUser(String userId);
	void removeUser(String uuid);
	String createDormantToken(String loginId);
	boolean isDormantToken(String token);
	String checkDormantToken(String token, String code);
}
