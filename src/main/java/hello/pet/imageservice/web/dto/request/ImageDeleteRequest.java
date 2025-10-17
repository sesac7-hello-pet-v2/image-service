package hello.pet.imageservice.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ImageDeleteRequest(
	@NotBlank(message = "삭제할 이미지의 키 값은 필수 입니다.")
	String deleteS3Key
) {
}
