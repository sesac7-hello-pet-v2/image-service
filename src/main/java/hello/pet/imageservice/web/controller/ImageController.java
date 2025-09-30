package hello.pet.imageservice.web.controller;

import org.springframework.http.ResponseEntity;

import hello.pet.imageservice.web.dto.request.ImageUploadRequest;
import hello.pet.imageservice.web.dto.response.ImageUploadResponse;

public interface ImageController {
	ResponseEntity<ImageUploadResponse> uploadImage(ImageUploadRequest uploadRequest);
}
