package hello.pet.imageservice.infrastructure.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import hello.pet.imageservice.infrastructure.exception.HelloPetException;
import hello.pet.imageservice.infrastructure.exception.HelloPetExceptionCode;
import hello.pet.imageservice.web.dto.response.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	/**
	 * HelloPetException을 처리하여 표준화된 에러 응답과 예외에 지정된 HTTP 상태를 반환한다.
	 *
	 * @param e 처리할 HelloPetException 인스턴스 — 응답의 HTTP 상태, 코드, 메시지를 제공한다.
	 * @return 응답 본문에 ExceptionResponse(예외의 상태, 코드, 메시지)를 담고 예외가 지정한 HTTP 상태를 설정한 ResponseEntity
	 */
	@ExceptionHandler(HelloPetException.class)
	public ResponseEntity<?> helloPetExceptionHandle(HelloPetException e) {
		log.error("HelloPetException occurred: {}", e.getMessage(), e);
		return ResponseEntity.status(e.getStatus())
			.body(
				ExceptionResponse.of(
					e.getStatus(), e.getCode(), e.getMessage()
				)
			);
	}

	/**
	 * 유효성 검사 실패 시 필드별 오류 메시지를 포함한 표준화된 에러 응답을 반환한다.
	 *
	 * 반환되는 응답의 HTTP 상태는 400 Bad Request이며, 응답 본문에는 검증 오류 코드 "VALIDATION_ERROR",
	 * 메시지 "입력값이 유효하지 않습니다.", 및 각 필드의 검증 오류 메시지를 담은 맵이 포함된다.
	 *
	 * @return HTTP 400 상태와 필드별 검증 오류 정보를 포함한 ExceptionResponse
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> validationExceptionHandler(MethodArgumentNotValidException e) {
		log.warn("Validation 실패: {}", e.getMessage());

		Map<String, String> errors = new HashMap<>();

		for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
			errors.put(fieldError.getField(), fieldError.getDefaultMessage());
		}

		return ResponseEntity
			.badRequest()
			.body(
				ExceptionResponse.of(
					HttpStatus.BAD_REQUEST,
					"VALIDATION_ERROR",
					"입력값이 유효하지 않습니다.",
					errors // 필드별 메시지 포함
				)
			);
	}

	/**
	 * 모든 예외를 처리하여 HTTP 500 내부 서버 오류 응답을 생성하고 반환한다.
	 *
	 * @return ResponseEntity - HTTP 500 상태와 INTERNAL_SERVER_ERROR 코드·메시지를 포함한 ExceptionResponse
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> exceptionHandler(Exception e) {
		HelloPetExceptionCode code = HelloPetExceptionCode.INTERNAL_SERVER_ERROR;
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(
				ExceptionResponse.of(
					code.getStatus(), code.getCode(), code.getMessage()
				)
			);
	}
}
