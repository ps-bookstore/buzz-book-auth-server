package store.buzzbook.authserver.service;


import java.util.Map;

public interface RedisService {
	void setKey(String key, String value);
	String getKey(String key);
	void saveUser(String userId, Map<String, Object> data);
	Map<String, Object> getUser(String userId);
	void removeUser(String uuid);
}
