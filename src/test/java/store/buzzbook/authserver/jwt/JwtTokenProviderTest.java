package store.buzzbook.authserver.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import store.buzzbook.authserver.dto.AuthDTO;
import store.buzzbook.authserver.dto.JwtResponse;
import store.buzzbook.authserver.service.RedisService;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private RedisService redisService;

    private JwtTokenProvider jwtTokenProvider;
    private AuthDTO authDTO;
    private String tokenKey;
    private String refreshTokenKey;

    @BeforeEach
    void setUp() {
        tokenKey = "sampleTokenKey12345678901234567890123456789012";
        refreshTokenKey = "sampleRefreshTokenKey12345678901234567890123456789012";
        jwtTokenProvider = new JwtTokenProvider(tokenKey, refreshTokenKey, redisService);

        authDTO = new AuthDTO();
        authDTO.setLoginId("testUser");
        authDTO.setRole("USER");
        authDTO.setUserId(1L);
    }

    @Test
    void generateTokenTest() {
        doNothing().when(redisService).saveUser(anyString(), any(Map.class));

        JwtResponse jwtResponse = jwtTokenProvider.generateToken(authDTO);

        assertThat(jwtResponse).isNotNull();
        assertThat(jwtResponse.getAccessToken()).isNotBlank();
        assertThat(jwtResponse.getRefreshToken()).isNotBlank();
    }

    @Test
    void validateTokenTest() {
        JwtResponse jwtResponse = jwtTokenProvider.generateToken(authDTO);

        boolean isValid = jwtTokenProvider.validateToken(jwtResponse.getAccessToken());
        assertThat(isValid).isTrue();
    }

    @Test
    void validateRefreshTokenTest() {
        JwtResponse jwtResponse = jwtTokenProvider.generateToken(authDTO);

        boolean isValid = jwtTokenProvider.validateRefreshToken(jwtResponse.getRefreshToken());
        assertThat(isValid).isTrue();
    }

    @Test
    void refreshAccessTokenTest() {
        JwtResponse jwtResponse = jwtTokenProvider.generateToken(authDTO);

        Map<String, Object> userData = new HashMap<>();
        userData.put("loginId", authDTO.getLoginId());
        userData.put("role", authDTO.getRole());
        userData.put("userId", authDTO.getUserId());
        when(redisService.getUser(anyString())).thenReturn(userData);
        doNothing().when(redisService).removeUser(anyString());

        JwtResponse newJwtResponse = jwtTokenProvider.refreshAccessToken(jwtResponse.getRefreshToken());

        assertThat(newJwtResponse).isNotNull();
        assertThat(newJwtResponse.getAccessToken()).isNotBlank();
        assertThat(newJwtResponse.getRefreshToken()).isNotBlank();

        boolean isValid = jwtTokenProvider.validateToken(newJwtResponse.getAccessToken());
        assertThat(isValid).isTrue();
    }


    @Test
    void refreshAccessTokenWithInvalidTokenTest() {
        String invalidRefreshToken = "invalidRefreshToken";

        JwtResponse newJwtResponse = jwtTokenProvider.refreshAccessToken(invalidRefreshToken);

        assertThat(newJwtResponse).isNull();
    }

    @Test
    void getUserInfoFromTokenTest() {
        JwtResponse jwtResponse = jwtTokenProvider.generateToken(authDTO);

        Map<String, Object> userData = new HashMap<>();
        userData.put("loginId", authDTO.getLoginId());
        userData.put("role", authDTO.getRole());
        userData.put("userId", authDTO.getUserId());
        when(redisService.getUser(anyString())).thenReturn(userData);

        Map<String, Object> userInfo = jwtTokenProvider.getUserInfoFromToken(jwtResponse.getAccessToken());

        assertThat(userInfo).isNotNull();
        assertThat(Optional.ofNullable(userInfo.get("loginId")).orElse("")).isEqualTo(authDTO.getLoginId());
        assertThat(Optional.ofNullable(userInfo.get("role")).orElse("")).isEqualTo(authDTO.getRole());
        assertThat(Optional.ofNullable(userInfo.get("userId")).orElse("")).isEqualTo(authDTO.getUserId());
    }

    @Test
    void getUUIDFromAccessTokenTest() {
        JwtResponse jwtResponse = jwtTokenProvider.generateToken(authDTO);

        String uuid = jwtTokenProvider.getUUIDFromAccessToken(jwtResponse.getAccessToken());

        assertThat(uuid).isNotNull();
    }

    @Test
    void getUUIDFromRefreshTokenTest() {
        JwtResponse jwtResponse = jwtTokenProvider.generateToken(authDTO);

        String uuid = jwtTokenProvider.getUUIDFromRefreshToken(jwtResponse.getRefreshToken());

        assertThat(uuid).isNotNull();
    }

    @Test
    void getUserInfoFromUUIDTest() {
        JwtResponse jwtResponse = jwtTokenProvider.generateToken(authDTO);

        String uuid = jwtTokenProvider.getUUIDFromAccessToken(jwtResponse.getAccessToken());

        Map<String, Object> userData = new HashMap<>();
        userData.put("loginId", authDTO.getLoginId());
        userData.put("role", authDTO.getRole());
        userData.put("userId", authDTO.getUserId());
        when(redisService.getUser(anyString())).thenReturn(userData);

        Map<String, Object> userInfo = jwtTokenProvider.getUserInfoFromUUID(uuid);

        assertThat(userInfo).isNotNull();
        assertThat(Optional.ofNullable(userInfo.get("loginId")).orElse("")).isEqualTo(authDTO.getLoginId());
        assertThat(Optional.ofNullable(userInfo.get("role")).orElse("")).isEqualTo(authDTO.getRole());
        assertThat(Optional.ofNullable(userInfo.get("userId")).orElse("")).isEqualTo(authDTO.getUserId());

    }

    @Test
    void validateTokenWithInvalidTokenTest() {
        String invalidToken = "invalidToken";

        boolean isValid = jwtTokenProvider.validateToken(invalidToken);
        assertThat(isValid).isFalse();
    }

    @Test
    void validateTokenWithExpiredTokenTest() {
        Key key = Keys.hmacShaKeyFor(tokenKey.getBytes());
        String expiredToken = Jwts.builder()
                .setSubject("testUser")
                .signWith(key, SignatureAlgorithm.HS256)
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .compact();

        boolean isValid = jwtTokenProvider.validateToken(expiredToken);
        assertThat(isValid).isFalse();
    }

    @Test
    void validateTokenWithUnsupportedTokenTest() {
        Key unsupportedKey = Keys.secretKeyFor(SignatureAlgorithm.HS384);
        String unsupportedToken = Jwts.builder()
                .setSubject("testUser")
                .signWith(unsupportedKey)
                .compact();

        boolean isValid = jwtTokenProvider.validateToken(unsupportedToken);
        assertThat(isValid).isFalse();
    }

    @Test
    void validateTokenWithEmptyTokenTest() {
        boolean isValid = jwtTokenProvider.validateToken("");
        assertThat(isValid).isFalse();
    }

    @Test
    void validateTokenWithInvalidSignatureTest() {
        Key invalidKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String tokenWithInvalidSignature = Jwts.builder()
                .setSubject("testUser")
                .signWith(invalidKey)
                .compact();

        assertThatThrownBy(() -> jwtTokenProvider.validateToken(tokenWithInvalidSignature))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("JWT 서명이 일치하지 않습니다.");
    }

    @Test
    void parseClaimsWithExpiredTokenTest() throws Exception {
        Key key = Keys.hmacShaKeyFor(tokenKey.getBytes());
        String expiredToken = Jwts.builder()
                .setSubject("testUser")
                .signWith(key, SignatureAlgorithm.HS256)
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .compact();

        // 리플렉션을 사용하여 private 메서드 호출
        Method parseClaimsMethod = JwtTokenProvider.class.getDeclaredMethod("parseClaims", String.class, Key.class);
        parseClaimsMethod.setAccessible(true);
        Claims claims = (Claims) parseClaimsMethod.invoke(jwtTokenProvider, expiredToken, key);

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("testUser");
    }
}
