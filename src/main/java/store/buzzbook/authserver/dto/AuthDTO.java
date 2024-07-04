package store.buzzbook.authserver.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class AuthDTO {
	private String loginId;
	private String role;
	private Long userId;
	private String accessToken;
	private String refreshToken;
}
