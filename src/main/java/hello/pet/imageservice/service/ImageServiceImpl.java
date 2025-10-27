package hello.pet.imageservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import hello.pet.imageservice.infrastructure.exception.HelloPetException;
import hello.pet.imageservice.infrastructure.exception.HelloPetExceptionCode;
import hello.pet.imageservice.web.dto.request.ImageDeleteRequest;
import hello.pet.imageservice.web.dto.request.ImageUploadRequest;
import hello.pet.imageservice.web.dto.response.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {
	private final S3Client s3Client;
	private final AsyncImageService asyncImageService;
	@Value("${spring.aws.s3.bucket-name}")
	private String bucketName;
	@Value("${spring.aws.s3.max-file-size}")
	private long maxFileSize;
	@Value("${spring.aws.s3.allowed-extensions}")
	private String allowedExtensions;

	@Override
	public ImageUploadResponse uploadImage(ImageUploadRequest request) {
		validateFile(request.file());
		String s3Key = generateS3Key(request);
		try {
			// 원본 이미지 S3 업로드
			uploadFileToS3(request.file(), s3Key);

			// 비동기로 리사이징된 이미지들 생성 및 업로드
			byte[] imageBytes = request.file().getBytes();
			asyncImageService.resizeAndUploadAsync(
				imageBytes,
				s3Key,
				request.file().getContentType()
			);

			return ImageUploadResponse.builder()
				.s3Key(s3Key)
				.build();
		} catch (S3Exception e) {
			log.error("S3 upload failed for image ID: {}", e.getMessage());
			throw new HelloPetException(HelloPetExceptionCode.FILE_PROCESS_ERROR);
		} catch (Exception e) {
			log.error("Unexpected error during image upload for ID: {}", e.getMessage());
			throw new HelloPetException(HelloPetExceptionCode.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public void deleteImage(ImageDeleteRequest request) {
		// S3 키가 유효한지 간단히 검증
		if (request.deleteS3Key() == null || request.deleteS3Key().trim().isEmpty()) {
			log.warn("Attempted to delete image with empty S3 key.");
			return; // 유효하지 않은 요청은 무시하거나, 필요시 예외 발생
		}

		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
			.bucket(bucketName)
			.key(request.deleteS3Key()) // 요청으로부터 받은 S3 Key 사용
			.build();

		try {
			s3Client.deleteObject(deleteObjectRequest);
			log.info("Successfully deleted S3 object with key: {}", request.deleteS3Key());
		} catch (S3Exception e) {
			// S3에서 객체 삭제 중 발생한 예외 처리 (예: 객체가 없는 경우 S3Exception 발생 가능)
			// 404 에러 등은 일반적으로 무시하지만, 로깅하여 추적합니다.
			log.error("S3 delete failed for key: {}. Status: {}", request.deleteS3Key(), e.statusCode(), e);
			throw new HelloPetException(HelloPetExceptionCode.FILE_PROCESS_ERROR); // 내부 오류 코드로 변환
		} catch (Exception e) {
			log.error("Unexpected error during image deletion for key: {}", request.deleteS3Key(), e);
			throw new HelloPetException(HelloPetExceptionCode.INTERNAL_SERVER_ERROR);
		}
	}

	private void uploadFileToS3(MultipartFile file, String s3Key) {
		try (InputStream is = file.getInputStream()) {
			// 안전한 파일명 생성 (메타데이터용)
			String safeFileName = sanitizeFileName(file.getOriginalFilename());

			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(s3Key)
				.contentType(file.getContentType())
				.contentLength(file.getSize())
				.metadata(Map.of(
					"original-filename", safeFileName,
					"upload-timestamp", String.valueOf(System.currentTimeMillis())
				))
				.build();

			s3Client.putObject(putObjectRequest,
				RequestBody.fromInputStream(is, file.getSize()));
		} catch (IOException e) {
			throw new HelloPetException(HelloPetExceptionCode.FILE_PROCESS_ERROR);
		}
	}

	private String generateS3Key(ImageUploadRequest request) {
		String imageName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5);
		String imageExtension = getFileExtension(request.file().getOriginalFilename());

		// 새로운 경로 구조: userId/type/postId/image.ext
		// postId가 없는 경우: userId/type/image.ext
		if (request.postId() == null || request.postId().trim().isEmpty()) {
			return String.format("%s/%s/%s.%s", request.userId(), request.type(), imageName, imageExtension);
		} else {
			return String.format("%s/%s/%s/%s.%s", request.userId(), request.type(), request.postId(), imageName,
				imageExtension);
		}
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new HelloPetException(HelloPetExceptionCode.FILE_IS_EMPTY);
		}

		if (file.getSize() > maxFileSize) {
			throw new HelloPetException(HelloPetExceptionCode.FILE_SIZE_BIG);
		}
		String fileName = file.getOriginalFilename();
		if (fileName == null || fileName.trim().isEmpty()) {
			throw new HelloPetException(HelloPetExceptionCode.FILE_NAME_IS_EMPTY);
		}

		// 파일명 길이 검증 (최대 255자로 제한)
		if (fileName.length() > 255) {
			log.warn("파일명이 너무 깁니다. 길이: {}, 파일명: {}", fileName.length(), fileName);
			// 파일명이 길어도 업로드는 허용하되 메타데이터 저장 시 잘라서 저장
		}

		String extension = getFileExtension(fileName).toLowerCase();
		List<String> allowedExtensionList = Arrays.asList(allowedExtensions.split(","));

		if (!allowedExtensionList.contains(extension)) {
			throw new HelloPetException(HelloPetExceptionCode.FILE_EXTENSION_NOT_ALLOW);
		}
	}

	private String getFileExtension(String fileName) {
		int lastDotIndex = fileName.lastIndexOf('.');
		return (lastDotIndex >= 0) ? fileName.substring(lastDotIndex + 1) : "";
	}

	/**
	 * 파일명을 안전하게 처리합니다.
	 * - 길이 제한 (200자로 제한하되 확장자는 보존)
	 * - 특수문자 제거
	 * - UTF-8 인코딩 안전성 보장
	 */
	private String sanitizeFileName(String originalFileName) {
		if (originalFileName == null || originalFileName.trim().isEmpty()) {
			return "unknown_file";
		}

		String fileName = originalFileName.trim();

		// 파일명에서 확장자 분리
		String baseName;
		String extension = "";
		int lastDotIndex = fileName.lastIndexOf('.');
		if (lastDotIndex > 0) {
			baseName = fileName.substring(0, lastDotIndex);
			extension = fileName.substring(lastDotIndex);
		} else {
			baseName = fileName;
		}

		// 특수문자 제거 (알파벳, 숫자, 한글, 공백, 하이픈, 언더스코어만 허용)
		baseName = baseName.replaceAll("[^a-zA-Z0-9가-힣\\s\\-_]", "");

		// 연속된 공백을 하나로 치환
		baseName = baseName.replaceAll("\\s+", " ");

		// 앞뒤 공백 제거
		baseName = baseName.trim();

		// 빈 문자열인 경우 기본값 설정
		if (baseName.isEmpty()) {
			baseName = "file_" + System.currentTimeMillis();
		}

		// 길이 제한 (확장자 포함해서 200자 이내)
		int maxBaseNameLength = 200 - extension.length();
		if (baseName.length() > maxBaseNameLength) {
			baseName = baseName.substring(0, maxBaseNameLength);
		}

		return baseName + extension;
	}
}
