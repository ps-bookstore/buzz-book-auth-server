package store.buzzbook.authserver.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class AuthRequest {
	private String username;
	private String role;
	private String session;
	private String token;
	private String refreshToken;

}
