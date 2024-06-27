package store.buzzbook.authserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.boot.test.context.SpringBootTest;

import store.buzzbook.authserver.jwt.JwtTokenProvider;

@SpringBootTest
class JwtTokenProviderTest {

	@InjectMocks
	private JwtTokenProvider jwtTokenProvider;

	private static final String SECRET_KEY = "f5aec30ed11e9cd92f23f391ddc24ae931e08ac1352487d178e0547e50bd17d1";
	private static final String REFRESH_KEY = "18fe93b955bf4861670b536214502d889288e3523f1ae02c6b28fae3b71f1bc3";

	@BeforeEach
	void setUp() {
		jwtTokenProvider = new JwtTokenProvider(SECRET_KEY, REFRESH_KEY);
	}

	@Test
	@DisplayName("토큰 생성")
	void createToken() {
		//given

		//when

		// then
	}
}
