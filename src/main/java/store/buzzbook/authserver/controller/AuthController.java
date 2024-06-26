package store.buzzbook.authserver.controller;

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

import store.buzzbook.authserver.dto.AuthRequest;
import store.buzzbook.authserver.dto.JwtResponse;
import store.buzzbook.authserver.jwt.JwtTokenProvider;

@RestController
@RequestMapping("/auth")
public class AuthController {
	private static final String BEARER_PREFIX = "Bearer ";
	private static final String BEARER_FORMAT = String.format("%s%s", BEARER_PREFIX, "?");

	private final JwtTokenProvider jwtTokenProvider;

	@Autowired
	public AuthController(JwtTokenProvider jwtTokenProvider) {
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@PostMapping("/token")
	public ResponseEntity<Void> generateToken(@RequestBody AuthRequest authRequest) {
		JwtResponse response = jwtTokenProvider.generateToken(authRequest);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", String.format(BEARER_FORMAT, response.getAccessToken()));
		headers.add("Refresh-Token", String.format(BEARER_FORMAT, response.getRefreshToken()));

		return ResponseEntity.ok().headers(headers).build();
	}

	@GetMapping("/role")
	public ResponseEntity<String> getRole(@RequestHeader(value = "Authorization", required = false) String accessToken,
		@RequestHeader(value = "Refresh-Token", required = false) String refreshToken) {
		if (accessToken == null || !accessToken.startsWith(BEARER_PREFIX) || refreshToken == null
			|| !refreshToken.startsWith(BEARER_PREFIX)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("다시 로그인해주세요.");
		}

		accessToken = accessToken.substring(BEARER_PREFIX.length());
		refreshToken = refreshToken.substring(BEARER_PREFIX.length());

		String role;
		if (!jwtTokenProvider.validateToken(accessToken)) { // access token 이 만료됐을 때
			JwtResponse response = jwtTokenProvider.refreshAccessToken(refreshToken);
			if (response != null) {
				HttpHeaders headers = new HttpHeaders();
				headers.add("Authorization", String.format(BEARER_FORMAT, response.getAccessToken()));
				headers.add("Refresh-Token", String.format(BEARER_FORMAT, response.getRefreshToken()));

				role = jwtTokenProvider.getRoleFromToken(response.getAccessToken());
				return ResponseEntity.ok().headers(headers).body(role);
			} else {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("다시 로그인해주세요.");
			}
		} else {
			// 만료되지 않았으면 바로 Role 을 반환
			role = jwtTokenProvider.getRoleFromToken(accessToken);
			return ResponseEntity.ok(role);
		}
	}
}
