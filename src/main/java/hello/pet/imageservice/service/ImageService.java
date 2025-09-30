package hello.pet.imageservice.service;

import hello.pet.imageservice.web.dto.request.ImageUploadRequest;
import hello.pet.imageservice.web.dto.response.ImageUploadResponse;

public interface ImageService {
	/**
 * 이미지 업로드를 처리하고 업로드 결과를 반환한다.
 *
 * @param request 업로드할 이미지 파일 및 관련 메타데이터를 포함한 요청 DTO
 * @return 업로드된 이미지의 식별자와 접근 정보 등을 담은 응답 DTO
 */
ImageUploadResponse uploadImage(ImageUploadRequest request);
}
