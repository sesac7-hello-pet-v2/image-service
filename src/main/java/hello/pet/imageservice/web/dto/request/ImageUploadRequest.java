package hello.pet.imageservice.web.dto.request;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

public record ImageUploadRequest(
	@NotNull(message = "사용자 ID는 필수입니다.")
	Long userId,
	String postId,
	@NotNull(message = "이미지 파일은 필수입니다.")
	MultipartFile file
) {
}
