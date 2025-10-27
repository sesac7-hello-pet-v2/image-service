package hello.pet.imageservice.web.dto.request;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ImageUploadRequest(
	@NotNull(message = "사용자 ID는 필수입니다.")
	@Schema(
		description = "사용자 ID",
		type = "Long",
		requiredMode = Schema.RequiredMode.REQUIRED
	)
	Long userId,

	@Schema(
		description = "게시글 ID (String 또는 Long)",
		type = "String",
		requiredMode = Schema.RequiredMode.NOT_REQUIRED
	)
	String postId,

	@NotNull(message = "이미지 타입은 필수입니다.")
	@Pattern(regexp = "^(pet|user|feed)$", message = "이미지 타입은 pet, user, feed 중 하나여야 합니다.")
	@Schema(
		description = "이미지 타입 (pet, user, feed)",
		type = "String",
		requiredMode = Schema.RequiredMode.REQUIRED,
		allowableValues = {"pet", "user", "feed"}
	)
	String type,

	@NotNull(message = "이미지 파일은 필수입니다.")
	@Schema(
		description = "이미지 파일",
		type = "MultipartFile",
		requiredMode = Schema.RequiredMode.REQUIRED
	)
	MultipartFile file
) {
}
