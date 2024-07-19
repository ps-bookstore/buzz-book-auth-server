package store.buzzbook.authserver.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import store.buzzbook.authserver.dto.AuthDTO;
import store.buzzbook.authserver.dto.JwtResponse;
import store.buzzbook.authserver.jwt.JwtTokenProvider;
import store.buzzbook.authserver.service.RedisService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private AuthController authController;


    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_FORMAT = "Bearer %s";
    private static final String TOKEN_HEADER = "Authorization";
    private static final String REFRESH_HEADER = "Refresh-Token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerateToken() {
        AuthDTO authDTO = new AuthDTO();
        JwtResponse jwtResponse = new JwtResponse("Bearer","accessToken", "refreshToken");

        when(jwtTokenProvider.generateToken(any(AuthDTO.class))).thenReturn(jwtResponse);

        ResponseEntity<Void> responseEntity = authController.generateToken(authDTO);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(String.format(TOKEN_FORMAT, jwtResponse.getAccessToken()), responseEntity.getHeaders().getFirst(TOKEN_HEADER));
        assertEquals(String.format(TOKEN_FORMAT, jwtResponse.getRefreshToken()), responseEntity.getHeaders().getFirst(REFRESH_HEADER));
    }

    @Test
    void testLogout() {
        String accessToken = "Bearer accessToken";
        String refreshToken = "Bearer refreshToken";

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUUIDFromAccessToken(anyString())).thenReturn("uuid");

        ResponseEntity<Void> responseEntity = authController.logout(accessToken, refreshToken);

        verify(redisService, times(1)).removeUser("uuid");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    void testLogoutWithInvalidTokens() {
        String accessToken = "Bearer accessToken";
        String refreshToken = "Bearer refreshToken";

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);
        when(jwtTokenProvider.validateRefreshToken(anyString())).thenReturn(false);

        ResponseEntity<Void> responseEntity = authController.logout(accessToken, refreshToken);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    void testGetUserInfo() {
        String accessToken = "Bearer accessToken";
        String refreshToken = "Bearer refreshToken";
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("user", "info");

        when(jwtTokenProvider.getUserInfoFromToken(anyString())).thenReturn(userInfo);

        ResponseEntity<Map<String, Object>> responseEntity = authController.getUserInfo(accessToken, refreshToken);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(userInfo, responseEntity.getBody());
    }

    @Test
    void testGetUserInfoWithRefreshToken() {
        String refreshToken = "Bearer refreshToken";
        JwtResponse jwtResponse = new JwtResponse("Bearer", "newAccessToken", "newRefreshToken");
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("user", "info");

        when(jwtTokenProvider.refreshAccessToken(anyString())).thenReturn(jwtResponse);
        when(jwtTokenProvider.getUserInfoFromToken(anyString())).thenReturn(userInfo);

        ResponseEntity<Map<String, Object>> responseEntity = authController.getUserInfo(null, refreshToken);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(userInfo, responseEntity.getBody());
        assertEquals(String.format(TOKEN_FORMAT, jwtResponse.getAccessToken()), responseEntity.getHeaders().getFirst(TOKEN_HEADER));
        assertEquals(String.format(TOKEN_FORMAT, jwtResponse.getRefreshToken()), responseEntity.getHeaders().getFirst(REFRESH_HEADER));
    }

    @Test
    void testGetDormantToken() {
        String loginId = "testLoginId";
        String token = "dormantToken";

        when(redisService.createDormantToken(anyString())).thenReturn(token);

        ResponseEntity<String> responseEntity = authController.getDormantToken(loginId);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(token, responseEntity.getBody());
    }

    @Test
    void testExistDormantToken() {
        String token = "dormantToken";

        when(redisService.isDormantToken(anyString())).thenReturn(true);

        ResponseEntity<Void> responseEntity = authController.existDormantToken(token);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    void testExistDormantTokenNotFound() {
        String token = "dormantToken";

        when(redisService.isDormantToken(anyString())).thenReturn(false);

        ResponseEntity<Void> responseEntity = authController.existDormantToken(token);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    void testCheckDormantToken() {
        String token = "dormantToken";
        String code = "123456";
        String loginId = "testLoginId";

        when(redisService.checkDormantToken(anyString(), anyString())).thenReturn(loginId);

        ResponseEntity<String> responseEntity = authController.checkDormantToken(token, code);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(loginId, responseEntity.getBody());
    }
}
