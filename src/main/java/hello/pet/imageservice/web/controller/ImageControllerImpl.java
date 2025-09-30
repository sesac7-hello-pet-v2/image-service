package hello.pet.imageservice.web.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hello.pet.imageservice.service.ImageService;
import hello.pet.imageservice.web.dto.request.ImageUploadRequest;
import hello.pet.imageservice.web.dto.response.ImageUploadResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/images")
@RequiredArgsConstructor
public class ImageControllerImpl implements ImageController {
	private final ImageService service;

	/**
	 * 이미지 업로드 요청을 처리하고 업로드 결과를 반환한다.
	 *
	 * @param uploadRequest 멀티파트 폼 데이터로 전송된 이미지 파일과 관련 메타데이터를 바인딩한 요청 DTO
	 * @return 업로드 결과를 담은 ImageUploadResponse 객체(업로드된 이미지 식별자, 접근 URL 등 메타데이터 포함)
	 */
	@Override
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ImageUploadResponse> uploadImage(@ModelAttribute ImageUploadRequest uploadRequest) {
		ImageUploadResponse response = service.uploadImage(uploadRequest);
		return ResponseEntity.ok(response);
	}
}
