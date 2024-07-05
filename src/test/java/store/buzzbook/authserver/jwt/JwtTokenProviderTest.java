package store.buzzbook.authserver.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import store.buzzbook.authserver.BuzzBookAuthServerApplication;
import store.buzzbook.authserver.dto.AuthDTO;
import store.buzzbook.authserver.dto.JwtResponse;

@SpringBootTest(
        classes = BuzzBookAuthServerApplication.class,
        properties = {
                "eureka.client.enabled=false", // Eureka 클라이언트를 비활성화
                "eureka.client.register-with-eureka=false", // Eureka 서버에 등록하지 않음
                "eureka.client.fetch-registry=false" // Eureka 레지스트리를 가져오지 않음
        }
)
@ActiveProfiles("dev") // 테스트 프로파일을 사용
public class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider; // JwtTokenProvider 주입

    private AuthDTO authDTO;

    @BeforeEach
    public void setUp() {
        // 각 테스트 전에 AuthDTO 객체 초기화
        authDTO = new AuthDTO();
        authDTO.setLoginId("testUser"); // 로그인 ID 설정
        authDTO.setRole("USER"); // 역할 설정
        authDTO.setUserId(1L); // 사용자 ID 설정
    }

    @Test
    public void generateTokenTest() {
        // JwtTokenProvider를 사용하여 JWT 토큰 생성
        JwtResponse jwtResponse = jwtTokenProvider.generateToken(authDTO);

        // 생성된 JWT 응답이 null이 아니고, accessToken과 refreshToken이 빈 문자열이 아님을 확인
        assertThat(jwtResponse).isNotNull();
        assertThat(jwtResponse.getAccessToken()).isNotBlank();
        assertThat(jwtResponse.getRefreshToken()).isNotBlank();
    }

    @Test
    public void validateTokenTest() {
        JwtResponse jwtResponse = jwtTokenProvider.generateToken(authDTO);

        boolean isValid = jwtTokenProvider.validateToken(jwtResponse.getAccessToken());
        assertThat(isValid).isTrue();
    }

    @Test
    public void validateRefreshTokenTest() {
        JwtResponse jwtResponse = jwtTokenProvider.generateToken(authDTO);

        boolean isValid = jwtTokenProvider.validateRefreshToken(jwtResponse.getRefreshToken());
        assertThat(isValid).isTrue();
    }

}
