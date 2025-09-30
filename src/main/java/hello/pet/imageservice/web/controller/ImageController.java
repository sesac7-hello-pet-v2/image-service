package hello.pet.imageservice.web.controller;

import static hello.pet.imageservice.infrastructure.exception.HelloPetExceptionCode.*;

import org.springframework.http.ResponseEntity;

import hello.pet.imageservice.infrastructure.config.swagger.annotation.ApiErrorCodeExamples;
import hello.pet.imageservice.web.dto.request.ImageUploadRequest;
import hello.pet.imageservice.web.dto.response.ImageUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public interface ImageController {
	@Operation(
		summary = "이미지 업로드",
		description = "이미지를 S3에 업로드를 수행하는 엔드포인트 입니다.<br>" +
			"이미지 업로드에 성공한 경우 S3상의 위치를 반환해 줍니다."
	)
	@ApiErrorCodeExamples({
		INTERNAL_SERVER_ERROR,
		S3_CONFIG_FAIL,
		FILE_IS_EMPTY,
		FILE_SIZE_BIG,
		FILE_NAME_IS_EMPTY,
		FILE_EXTENSION_NOT_ALLOW,
		FILE_PROCESS_ERROR
	})
	@ApiResponse(responseCode = "200", description = "OK - 이미지 업로드에 성공하였습니다.")
	ResponseEntity<ImageUploadResponse> uploadImage(ImageUploadRequest uploadRequest);
}
