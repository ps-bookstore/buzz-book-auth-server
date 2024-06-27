package store.buzzbook.authserver.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import store.buzzbook.authserver.dto.RedisRequest;
import store.buzzbook.authserver.service.impl.RedisServiceImpl;

@RestController
@RequiredArgsConstructor
public class RedisController {
	private final RedisServiceImpl redisService;

	@PostMapping("/redis/test")
	public ResponseEntity<?> addRedisKey(@RequestBody RedisRequest request) {
		redisService.setKey("username", request.getUsername());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@GetMapping("/redis/test/{key}")
	public ResponseEntity<?> getRedisKey(@PathVariable String key) {
		String value = redisService.getKey(key);
		return new ResponseEntity<>(value, HttpStatus.OK);
	}
}
