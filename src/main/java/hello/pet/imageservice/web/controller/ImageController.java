package hello.pet.imageservice.web.controller;

import static hello.pet.imageservice.infrastructure.exception.HelloPetExceptionCode.*;

import org.springframework.http.ResponseEntity;

import hello.pet.imageservice.infrastructure.config.swagger.annotation.ApiErrorCodeExamples;
import hello.pet.imageservice.web.dto.request.ImageUploadRequest;
import hello.pet.imageservice.web.dto.response.ImageUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public interface ImageController {
	/**
	 * 이미지 파일을 업로드하고 업로드된 파일의 S3 위치 정보를 응답으로 반환합니다.
	 *
	 * @param uploadRequest 업로드할 이미지와 관련된 요청 데이터
	 * @return 업로드에 성공한 이미지의 S3 위치를 포함하는 응답 엔티티
	 */
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
