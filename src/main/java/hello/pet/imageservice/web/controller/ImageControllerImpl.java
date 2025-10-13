package hello.pet.imageservice.web.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hello.pet.imageservice.service.ImageService;
import hello.pet.imageservice.web.dto.request.ImageDeleteRequest;
import hello.pet.imageservice.web.dto.request.ImageUploadRequest;
import hello.pet.imageservice.web.dto.response.ImageUploadResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/v1/images")
@RequiredArgsConstructor
public class ImageControllerImpl implements ImageController {
	private final ImageService service;

	@Override
	@PostMapping(value = "/post/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ImageUploadResponse> uploadImage(@Valid @ModelAttribute ImageUploadRequest uploadRequest) {
		ImageUploadResponse response = service.uploadImage(uploadRequest);
		return ResponseEntity.ok(response);
	}

	@Override
	@DeleteMapping
	public ResponseEntity<?> deleteImage(@Valid @RequestBody ImageDeleteRequest request) {
		service.deleteImage(request);
		return ResponseEntity.ok().build();
	}
}
