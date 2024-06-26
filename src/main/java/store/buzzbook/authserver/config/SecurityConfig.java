package store.buzzbook.authserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import store.buzzbook.authserver.jwt.JwtTokenProvider;

/**
 * Spring Security 설정을 구성하는 클래스입니다.
 *
 * @author 김성호
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider) throws
		Exception {

		// csrf disable
		http.csrf(AbstractHttpConfigurer::disable);

		// Form 로그인 방식 disable
		http.formLogin(AbstractHttpConfigurer::disable);

		// http basic 인증방식 disable
		http.httpBasic(AbstractHttpConfigurer::disable);

		//경로 별 인가 작업
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers("/admin").hasRole("ADMIN")
			.requestMatchers("/user/**").hasRole("USER")
			.requestMatchers("/", "/auth/**").permitAll()
		);

		// 세션 설정 (세션이 아닌 jwt 토큰을 사용할거기 때문에 STATELESS 설정 필수)
		http.sessionManagement(session -> session
			.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		return http.build();
	}
}
