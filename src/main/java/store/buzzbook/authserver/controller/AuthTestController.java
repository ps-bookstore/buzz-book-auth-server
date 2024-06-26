package store.buzzbook.authserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthTestController {

	@GetMapping("/")
	public ResponseEntity<String> mainApplication() {
		StringBuilder result = new StringBuilder();
		result.append("모든 사용자가 접근할 수 있는 테스트용 페이지 입니다.");
		result.append("\n");
		result.append("토큰이 없는 사용자, user 권한이 있는 사용자, admin 권한이 있는 사용자 모두 접근 가능한 페이지입니다.");

		return ResponseEntity.ok(result.toString());
	}

	@GetMapping("/admin")
	public ResponseEntity<String> adminApplication() {
		StringBuilder result = new StringBuilder();
		result.append("어드민 권한이 있는 사용자만 접근할 수 있는 테스트용 페이지 입니다.");
		result.append("\n");
		result.append("토큰이 필요하고 토큰에 admin 권한이 포함되어 있어야 합니다.");

		return ResponseEntity.ok(result.toString());
	}

	@GetMapping("/user/{userid}")
	public ResponseEntity<String> userApplication(@PathVariable("userid") String userid) {
		StringBuilder result = new StringBuilder();
		result.append("User ID가 일치하는 사람만 접근할 수 있는 테스트용 페이지 입니다.");
		result.append("\n");
		result.append("토큰이 필요하고 토큰에 user 정보가 있어야 합니다.");
		result.append("\n");
		result.append(String.format("User ID : %s", userid));
		// todo admin 도 접근가능해야하나?

		return ResponseEntity.ok(result.toString());

	}
}
