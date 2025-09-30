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
	 * 클라이언트의 멀티파트 이미지 업로드 요청을 처리하고 업로드 결과를 응답합니다.
	 *
	 * @param uploadRequest 멀티파트 폼 데이터로 바인딩된 업로드 요청 DTO(업로드할 파일과 관련 메타데이터 포함)
	 * @return 업로드 결과를 담은 ImageUploadResponse를 본문으로 포함하는 HTTP 200 OK 응답
	 */
	@Override
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ImageUploadResponse> uploadImage(@ModelAttribute ImageUploadRequest uploadRequest) {
		ImageUploadResponse response = service.uploadImage(uploadRequest);
		return ResponseEntity.ok(response);
	}
}
