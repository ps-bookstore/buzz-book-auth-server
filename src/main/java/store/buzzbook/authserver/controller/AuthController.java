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
import store.buzzbook.authserver.service.RedisService;

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
    private final RedisService redisService;

    @Autowired
    public AuthController(JwtTokenProvider jwtTokenProvider, RedisService redisService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisService = redisService;
    }

    /**
     * 토큰을 발급
     */
    @PostMapping("/token")
    public ResponseEntity<Void> generateToken(@RequestBody AuthDTO authDTO) {
        JwtResponse response = jwtTokenProvider.generateToken(authDTO);

        HttpHeaders headers = new HttpHeaders();
        headers.add(TOKEN_HEADER, String.format(TOKEN_FORMAT, response.getAccessToken()));
        headers.add(REFRESH_HEADER, String.format(TOKEN_FORMAT, response.getRefreshToken()));

        return ResponseEntity.ok().headers(headers).build();
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = TOKEN_HEADER, required = false) String accessToken,
            @RequestHeader(value = REFRESH_HEADER, required = false) String refreshToken) {

        if (accessToken == null && refreshToken == null) {
            // accessToken 과 refreshToken 둘 다 null 인 경우 처리
            log.warn("토큰 정보가 없습니다.");
            return ResponseEntity.badRequest().build();
        }

        // 엑세스 토큰이 만료되었는지 확인
        if (jwtTokenProvider.validateToken(accessToken)) {
            // 액세스 토큰이 유효하면 바로 사용자 정보 삭제
            String uuid = jwtTokenProvider.getUUIDFromAccessToken(accessToken);
            redisService.removeUser(uuid);
            log.debug("엑세스 토큰으로 로그아웃 함");
        } else if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            // 리프레시 토큰이 만료되지 않았을 때
            String userId = jwtTokenProvider.getUUIDFromRefreshToken(refreshToken);
            redisService.removeUser(userId);
            log.debug("리프레시토큰 으로 로그아웃 함");
        } else {
            // 액세스 토큰 리프레시토큰 둘 다 만료됐을 때는 아무처리도안함
            // 레디스에 유저정보를 저장할 때 00일 뒤 자동 삭제처리를 해놨기 때문
            log.debug("토큰이 만료됐기 때문에 자동 로그아웃 처리됨");
        }
        return ResponseEntity.ok().build();


    }

    /**
     * user info 을 반환해줌
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getUserInfo(
            @RequestHeader(value = TOKEN_HEADER, required = false) String accessToken,
            @RequestHeader(value = REFRESH_HEADER, required = false) String refreshToken) {
        try {
            // refresh 토큰에 Bearer 가 없을 때
            if (isTokenPresentAndValid(refreshToken)) {
                Map<String, Object> result = new HashMap<>();
                result.put("message", "다시 로그인해주세요.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
            }
            refreshToken = extractToken(refreshToken);

            // access token 이 만료 됐을 때
            if (isTokenPresentAndValid(accessToken)) {
                // 토큰 재발급 시작
                JwtResponse response = jwtTokenProvider.refreshAccessToken(refreshToken);
                if(response != null) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(TOKEN_HEADER, String.format(TOKEN_FORMAT, response.getAccessToken()));
                    headers.add(REFRESH_HEADER, String.format(TOKEN_FORMAT, response.getRefreshToken()));

                    Map<String, Object> userInfo = jwtTokenProvider.getUserInfoFromToken(response.getAccessToken());
                    log.debug("토큰 재발급 되고 user 정보를 body 에 줌");
                    return ResponseEntity.ok().headers(headers).body(userInfo);
                } else {
                    Map<String, Object> result = new HashMap<>();
                    result.put("message", "다시 로그인해주세요.");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
                }
            } else {
                // access token 이 만료되지 않았을 때
                accessToken = extractToken(accessToken);
                Map<String, Object> userInfo = jwtTokenProvider.getUserInfoFromToken(accessToken);
                return ResponseEntity.ok().body(userInfo);
            }
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "다시 로그인해주세요.");
            result.put("error", e.getMessage());
            log.debug("error {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    private boolean isTokenPresentAndValid(String token) {
        return token == null || !token.startsWith(BEARER_PREFIX);
    }

    private String extractToken(String token) {
        return token.substring(BEARER_PREFIX.length());
    }
}
