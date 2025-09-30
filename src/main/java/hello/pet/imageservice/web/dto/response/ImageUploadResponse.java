package hello.pet.imageservice.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ImageUploadResponse(
	@Schema(
		description = "S3 상의 이미지 접근 KEY",
		type = "String"
	)
	String s3Key
) {
}
