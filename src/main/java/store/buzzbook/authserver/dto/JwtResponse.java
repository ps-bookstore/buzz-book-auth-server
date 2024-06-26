package store.buzzbook.authserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 인증타입, 액세스 토큰, 리프레시 토큰을 포함하는 JWT 응답입니다.
 *
 * @author 김성호
 */
@AllArgsConstructor
@Builder
@Data
public class JwtResponse {
	/**
	 * JWT 인증타입 (ex "Bearer").
	 */
	private String grantType;
	private String accessToken;
	private String refreshToken;
}
