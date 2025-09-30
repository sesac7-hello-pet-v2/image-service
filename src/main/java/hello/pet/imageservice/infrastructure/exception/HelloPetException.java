package hello.pet.imageservice.infrastructure.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class HelloPetException extends RuntimeException {
	private final HttpStatus status;
	private final String code;
	private final String message;

	/**
	 * 제공된 HelloPetExceptionCode로부터 HTTP 상태, 애플리케이션 코드 및 메시지를 가져와 HelloPetException 인스턴스를 생성하고 해당 필드를 초기화한다.
	 *
	 * @param code HTTP 상태, 에러 코드 문자열 및 사용자 메시지를 제공하는 HelloPetExceptionCode 객체
	 */
	public HelloPetException(HelloPetExceptionCode code) {
		this.status = code.getStatus();
		this.code = code.getCode();
		this.message = code.getMessage();
	}
}
