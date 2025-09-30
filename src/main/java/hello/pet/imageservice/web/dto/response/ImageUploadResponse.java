package hello.pet.imageservice.web.dto.response;

import lombok.Builder;

@Builder
public record ImageUploadResponse(
	String s3Key
) {
}
