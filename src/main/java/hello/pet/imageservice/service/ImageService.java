package hello.pet.imageservice.service;

import hello.pet.imageservice.web.dto.request.ImageDeleteRequest;
import hello.pet.imageservice.web.dto.request.ImageUploadRequest;
import hello.pet.imageservice.web.dto.response.ImageUploadResponse;

public interface ImageService {
	ImageUploadResponse uploadImage(ImageUploadRequest request);

	void deleteImage(ImageDeleteRequest request);
}
