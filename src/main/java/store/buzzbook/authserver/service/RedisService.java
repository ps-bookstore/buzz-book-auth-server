package store.buzzbook.authserver.service;


public interface RedisService {
	void setKey(String key, String value);
	String getKey(String key);
}
