package hello.pet.imageservice.infrastructure.exception;

import java.lang.reflect.Field;
import java.util.Objects;

import org.springframework.http.HttpStatus;

import hello.pet.imageservice.infrastructure.config.swagger.annotation.ExplainError;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HelloPetExceptionCode {
	@ExplainError("서버 내부 오류가 발생했을 때 반환됩니다.")
	INTERNAL_SERVER_ERROR(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"INTERNAL_SERVER_ERROR",
		"알 수 없는 서버 에러가 발생하였습니다."
	),
	@ExplainError("AWS S3 초기 설정 시 오류가 발생하면 발생할 에러 입니다.")
	S3_CONFIG_FAIL(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"S3_CONFIG_FAIL",
		"AWS의 S3 설정 중 알 수 없는 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	/**
	 * 열거형 상수에 연결된 `@ExplainError` 애너테이션의 설명 문구를 반환합니다.
	 *
	 * @return 애너테이션의 `value()`에 해당하는 설명 문자열. 해당 애너테이션이 없으면 enum 인스턴스의 `message` 값을 반환합니다.
	 * @throws NoSuchFieldException 현재 열거형 상수에 대응하는 필드를 찾을 수 없을 경우 발생합니다.
	 */
	public String getExplainError() throws NoSuchFieldException {
		// 1. CoTaskExceptionCode 클래스에서 현재 enum 상수의 필드를 찾음
		Field field = HelloPetExceptionCode.class.getField(this.name());
		ExplainError annotation = field.getAnnotation(ExplainError.class);
		return Objects.nonNull(annotation) ? annotation.value() : this.message;
	}
}
