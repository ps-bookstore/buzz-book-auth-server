package store.buzzbook.authserver.jwt;

import java.security.Key;
import java.security.SignatureException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import store.buzzbook.authserver.dto.AuthRequest;
import store.buzzbook.authserver.dto.JwtResponse;

@Slf4j
@Component
public class JwtTokenProvider {
	private final Key tokenKey;
	private final Key refreshTokenKey;

	public JwtTokenProvider(@Value("${jwt.secret}") String tokenKey,
		@Value("${jwt.refresh}") String refreshTokenKey) {
		this.tokenKey = Keys.hmacShaKeyFor(tokenKey.getBytes());
		this.refreshTokenKey = Keys.hmacShaKeyFor(refreshTokenKey.getBytes());
	}

	/** 인증(Authentication) 객체를 기반으로 Access Token + Refresh Token 생성 */
	public JwtResponse generateToken(AuthRequest authRequest) {

		// ZonedDateTime 객체의 현재 시간을 long 타입의 epoch milli 로 변환
		long now = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

		// Access Token 생성
		Date accessTokenExpiresIn = new Date(now + 60 * 30 * 1000); // 30분
		String accessToken = Jwts.builder()
			.setSubject(authRequest.getUsername()) // subSubject 는 JWT 의 주체를 의미
			.claim("auth", authRequest.getRole())
			.setExpiration(accessTokenExpiresIn)
			.signWith(tokenKey, SignatureAlgorithm.HS256)
			.compact();

		// Refresh Token 생성
		String refreshToken = Jwts.builder()
			.setExpiration(new Date(now + 60 * 60 * 24 * 1000)) // 24시간(1일)
			.signWith(refreshTokenKey, SignatureAlgorithm.HS256)
			.compact();

		return JwtResponse.builder()
			.grantType("Bearer")
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}

	/** Access Token 갱신 */
	public JwtResponse refreshAccessToken(String refreshToken) {
		if (validateRefreshToken(refreshToken)) {
			Claims claims = parseClaims(refreshToken);

			String username = claims.getSubject();
			String role = claims.get("auth", String.class);

			AuthRequest authRequest = new AuthRequest();
			authRequest.setUsername(username);
			authRequest.setRole(role);

			return generateToken(authRequest);
		} else {
			return null; // Refresh Token 이 유효하지 않으면 null 반환
		}
	}

	/** 토큰 정보를 검증하여 유효성 확인 */
	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder()
				.setSigningKey(tokenKey)
				.build()
				.parseClaimsJws(token);
			return true;
		} catch (SecurityException | MalformedJwtException e) {
			log.debug("잘못된 JWT Token 입니다.", e);
		} catch (ExpiredJwtException e) {
			log.debug("만료된 JWT Token 입니다.", e);
		} catch (UnsupportedJwtException e) {
			log.debug("지원하지 않는 JWT Token 입니다.", e);
		} catch (IllegalArgumentException e) {
			log.debug("JWT Token 정보가 비어있습니다.", e);
		} catch (io.jsonwebtoken.security.SignatureException e) {
			log.debug("JWT 서명이 일치하지 않습니다.", e);
			throw new RuntimeException("JWT 서명이 일치하지 않습니다.");
		}
		return false;
	}

	/** Refresh Token 정보를 검증하여 유효성 확인 */
	public boolean validateRefreshToken(String token) {
		try {
			Jwts.parserBuilder()
				.setSigningKey(refreshTokenKey)
				.build()
				.parseClaimsJws(token);
			return true;
		} catch (SecurityException | MalformedJwtException e) {
			log.debug("잘못된 JWT Token 입니다.", e);
		} catch (ExpiredJwtException e) {
			log.debug("만료된 JWT Token 입니다.", e);
		} catch (UnsupportedJwtException e) {
			log.debug("지원하지 않는 JWT Token 입니다.", e);
		} catch (IllegalArgumentException e) {
			log.debug("JWT Token 정보가 비어있습니다.", e);
		} catch (io.jsonwebtoken.security.SignatureException e) {
			log.debug("JWT 서명이 일치하지 않습니다.", e);
			throw new RuntimeException("JWT 서명이 일치하지 않습니다.");
		}
		return false;
	}

	/** 주어진 Access Token 을 복호화 하고, 만료된 토큰인 경우에도 Claims 반환 */
	private Claims parseClaims(String token) {
		try {
			return Jwts.parserBuilder()
				.setSigningKey(tokenKey)
				.build()
				.parseClaimsJws(token)
				.getBody();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		}
	}

	/** 주어진 Access Token 에서 역할 정보를 추출 */
	public String getRoleFromToken(String token) {
		Claims claims = parseClaims(token);
		return claims.get("auth", String.class);
	}
}
