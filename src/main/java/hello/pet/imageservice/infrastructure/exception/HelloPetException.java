package hello.pet.imageservice.infrastructure.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class HelloPetException extends RuntimeException {
	private final HttpStatus status;
	private final String code;
	private final String message;

	/**
	 * 전달된 HelloPetExceptionCode로부터 상태, 에러 코드 및 메시지를 설정하여 HelloPetException 인스턴스를 생성한다.
	 *
	 * @param code HTTP 상태, 에러 코드 문자열 및 사용자 메시지를 제공하는 HelloPetExceptionCode 객체
	 */
	public HelloPetException(HelloPetExceptionCode code) {
		this.status = code.getStatus();
		this.code = code.getCode();
		this.message = code.getMessage();
	}
}
