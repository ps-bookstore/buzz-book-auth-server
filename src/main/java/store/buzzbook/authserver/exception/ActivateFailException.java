package store.buzzbook.authserver.exception;

public class ActivateFailException extends RuntimeException {
	public ActivateFailException(String message) {
		super(message);
	}
	public ActivateFailException() {
		super("존재하지 않는 활성화 토큰이거나 잘못된 코드를 입력했습니다.");
	}
}
