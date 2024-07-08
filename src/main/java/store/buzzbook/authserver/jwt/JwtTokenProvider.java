package store.buzzbook.authserver.jwt;

import java.security.Key;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import store.buzzbook.authserver.dto.AuthDTO;
import store.buzzbook.authserver.dto.JwtResponse;
import store.buzzbook.authserver.service.RedisService;

@Slf4j
@Component
public class JwtTokenProvider {
    private final Key tokenKey;
    private final Key refreshTokenKey;
    private final RedisService redisService;

    public JwtTokenProvider(@Value("${jwt.secret}") String tokenKey,
                            @Value("${jwt.refresh}") String refreshTokenKey,
                            RedisService redisService) {
        this.tokenKey = Keys.hmacShaKeyFor(tokenKey.getBytes());
        this.refreshTokenKey = Keys.hmacShaKeyFor(refreshTokenKey.getBytes());
        this.redisService = redisService;
    }

    /**
     * 인증(Authentication) 객체를 기반으로 Access Token + Refresh Token 생성
     */
    public JwtResponse generateToken(AuthDTO authDTO) {
        // 현재 시간을 기준으로 30분 후와 24시간 후의 시간을 계산
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        Date issuedAt = Date.from(now.toInstant()); // 현재
        Date accessTokenExpiresIn = Date.from(now.plusMinutes(30).toInstant()); // 30분
        Date refreshTokenExpiresIn = Date.from(now.plusDays(1).toInstant()); // 1일

        UUID uuid = UUID.randomUUID();
        String accessToken = Jwts.builder()
                .setSubject(uuid.toString()) // subSubject 는 JWT 의 주체를 의미
                .setIssuedAt(issuedAt) // 발급시간
                .setExpiration(accessTokenExpiresIn) // 유효기간
                .claim("sub", "access_token")
                .claim("user_id", uuid.toString())
                .signWith(tokenKey, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .setIssuedAt(issuedAt)
                .setExpiration(refreshTokenExpiresIn) // 24시간(1일)
                .claim("sub", "refresh_token")
                .claim("user_id", uuid.toString())
                .signWith(refreshTokenKey, SignatureAlgorithm.HS256)
                .compact();

        // 로그 추가
        log.debug("Generated Access Token: {}", accessToken);
        log.debug("Generated Refresh Token: {}", refreshToken);

        // Redis에 사용자 데이터 저장
        Long userId = authDTO.getUserId();
        String role = authDTO.getRole();
        String loginId = authDTO.getLoginId();
        Map<String, Object> userData = new HashMap<>();
        userData.put("loginId", loginId);
        userData.put("role", role);
        userData.put("userId", userId);
        redisService.saveUser(uuid.toString(), userData);

        return JwtResponse.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Token 갱신
     */
    public JwtResponse refreshAccessToken(String refreshToken) {
        // refresh token 이 만료되지 않았을 때만 access token 갱신 가능
        log.debug("Validating Refresh Token: {}", refreshToken);
        if (validateRefreshToken(refreshToken)) {
            Claims claims = parseClaims(refreshToken, refreshTokenKey);
            String uuid = claims.get("user_id").toString();

            // Redis에서 사용자 데이터 가져오기
            Map<String, Object> userData = redisService.getUser(uuid);
            // 재발급 하기 때문에 이전 정보 삭제 return generateToken(authDTO);  부분에서 레디스 저장 다시 됨
            redisService.removeUser(uuid);

            String loginId = (String) userData.get("loginId");
            String role = (String) userData.get("role");
            Long userId = ((Integer) userData.get("userId")).longValue();

            log.debug("user 정보 확인 {}, {}, {}, {}", uuid, loginId, role, userId);

            AuthDTO authDTO = new AuthDTO();
            authDTO.setLoginId(loginId);
            authDTO.setRole(role);
            authDTO.setUserId(userId);

            return generateToken(authDTO);
        } else {
            return null; // Refresh Token 이 유효하지 않으면 null 반환
        }
    }

    /**
     * 토큰 정보를 검증하여 유효성 확인
     */
    public boolean validateToken(String token) {
        return validateTokenWithKey(token, tokenKey);
    }

    /**
     * Refresh Token 정보를 검증하여 유효성 확인
     */
    public boolean validateRefreshToken(String token) {
        return validateTokenWithKey(token, refreshTokenKey);
    }

    private boolean validateTokenWithKey(String token, Key key) {
        try {
            token = token.trim();
            Jwts.parserBuilder()
                    .setSigningKey(key)
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
        } catch (RuntimeException e) {
            log.debug("RuntimeException {}", e.getMessage());
        }
        return false;
    }

    /**
     * 주어진 Token 을 복호화 하고, 만료된 토큰인 경우에도 Claims 반환
     */
    private Claims parseClaims(String token, Key key) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     * 주어진 access Token 에서 유저정보를 추출
     */
    public Map<String, Object> getUserInfoFromToken(String token) {
        Claims claims = parseClaims(token, tokenKey);
        String uuid = claims.get("user_id", String.class);
        return getUserInfoFromUUID(uuid);
    }

    /**
     * access token 에서 uuid만 꺼내기
     */
    public String getUUIDFromAccessToken(String token) {
        Claims claims = parseClaims(token, tokenKey);
        return claims.get("user_id", String.class);
    }

    /**
     * refresh token 에서 uuid 만 꺼내기 access token 재발급 용
     */
    public String getUUIDFromRefreshToken(String refreshToken) {
        Claims claims = parseClaims(refreshToken, refreshTokenKey);
        return claims.get("user_id", String.class);
    }

    /**
     * uuid 로 redis 에서 유저정보 가져오기
     */
    public Map<String, Object> getUserInfoFromUUID(String uuid) {
        return redisService.getUser(uuid);
    }
}
