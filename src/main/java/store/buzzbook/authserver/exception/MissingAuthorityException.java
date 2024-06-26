package store.buzzbook.authserver.exception;

/**
 * 사용자 정의 예외 클래스입니다. 권한 정보가 없는 토큰을 처리할 때 사용됩니다.
 */
public class MissingAuthorityException extends RuntimeException {
	public MissingAuthorityException(String message) {
		super(message);
	}
}
