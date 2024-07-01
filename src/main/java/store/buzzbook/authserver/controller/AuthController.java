package store.buzzbook.authserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import store.buzzbook.authserver.dto.AuthDTO;
import store.buzzbook.authserver.dto.JwtResponse;
import store.buzzbook.authserver.jwt.JwtTokenProvider;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final String TOKEN_FORMAT = "Bearer %s";
	private static final String TOKEN_HEADER = "Authorization";
	private static final String REFRESH_HEADER = "Refresh-Token";

	private final JwtTokenProvider jwtTokenProvider;

	@Autowired
	public AuthController(JwtTokenProvider jwtTokenProvider) {
		this.jwtTokenProvider = jwtTokenProvider;
	}

	/** 토큰을 발급 */
	@PostMapping("/token")
	public ResponseEntity<Void> generateToken(@RequestBody AuthDTO authDTO) {
		JwtResponse response = jwtTokenProvider.generateToken(authDTO);

		HttpHeaders headers = new HttpHeaders();
		headers.add(TOKEN_HEADER, String.format(TOKEN_FORMAT, response.getAccessToken()));
		headers.add(REFRESH_HEADER, String.format(TOKEN_FORMAT, response.getRefreshToken()));

		return ResponseEntity.ok().headers(headers).build();
	}

	/** user info 을 반환해줌 */
	@GetMapping("/info")
	public ResponseEntity<Map<String, Object>> getUserInfo(
			@RequestHeader(value = TOKEN_HEADER, required = false) String accessToken,
			@RequestHeader(value = REFRESH_HEADER, required = false) String refreshToken) {
		try {
			if (!isTokenPresentAndValid(accessToken) || !isTokenPresentAndValid(refreshToken)) {
				Map<String, Object> result = new HashMap<>();
				result.put("message", "다시 로그인해주세요.");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
			}

			accessToken = extractToken(accessToken);
			refreshToken = extractToken(refreshToken);

			if (!jwtTokenProvider.validateToken(accessToken)) { // access token 이 만료됐을 때
				JwtResponse response = jwtTokenProvider.refreshAccessToken(accessToken, refreshToken);
				if (response != null) {
					HttpHeaders headers = new HttpHeaders();
					headers.add(TOKEN_HEADER, String.format(TOKEN_FORMAT, response.getAccessToken()));
					headers.add(REFRESH_HEADER, String.format(TOKEN_FORMAT, response.getRefreshToken()));

					Map<String, Object> userInfo = jwtTokenProvider.getUUIDFromToken(response.getAccessToken());
					log.debug("토큰 재발급 되고 user 정보를 body 에 줌");
					return ResponseEntity.ok().headers(headers).body(userInfo);
				} else {
					Map<String, Object> result = new HashMap<>();
					result.put("message", "다시 로그인해주세요.");
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
				}
			} else {
				// 만료되지 않았으면 바로 Role 을 반환
				Map<String, Object> userInfo = jwtTokenProvider.getUUIDFromToken(accessToken);
				return ResponseEntity.ok().body(userInfo);
			}
		} catch (Exception e) {
			Map<String, Object> result = new HashMap<>();
			result.put("message", "다시 로그인해주세요.");
			result.put("error", e.getMessage());

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
		}
	}

	private boolean isTokenPresentAndValid(String token) {
		return token != null && token.startsWith(BEARER_PREFIX);
	}

	private String extractToken(String token) {
		return token.substring(BEARER_PREFIX.length());
	}
}
