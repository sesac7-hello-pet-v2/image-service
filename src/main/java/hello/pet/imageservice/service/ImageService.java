package hello.pet.imageservice.service;

import hello.pet.imageservice.web.dto.request.ImageUploadRequest;
import hello.pet.imageservice.web.dto.response.ImageUploadResponse;

public interface ImageService {
	/**
 * 이미지를 업로드하고 업로드 결과를 반환한다.
 *
 * @param request 업로드할 이미지 데이터와 관련 메타데이터(파일, 콘텐츠 타입, 대상 경로 등)를 담은 요청 객체
 * @return 업로드된 이미지의 식별자, 접근 URL 및 관련 메타정보를 포함한 응답 객체
 */
ImageUploadResponse uploadImage(ImageUploadRequest request);
}
