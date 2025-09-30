package hello.pet.imageservice.service;

import hello.pet.imageservice.web.dto.request.ImageUploadRequest;
import hello.pet.imageservice.web.dto.response.ImageUploadResponse;

public interface ImageService {
	/**
 * 이미지 업로드 요청을 처리하고 업로드 결과를 반환한다.
 *
 * @param request 업로드할 이미지와 관련 메타데이터를 포함한 요청 DTO
 * @return 업로드된 이미지의 식별자 및 접근 정보 등 업로드 결과를 담은 ImageUploadResponse 객체
 */
ImageUploadResponse uploadImage(ImageUploadRequest request);
}
