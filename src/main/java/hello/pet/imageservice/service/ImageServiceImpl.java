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
import hello.pet.imageservice.web.dto.request.ImageUploadRequest;
import hello.pet.imageservice.web.dto.response.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {
	private final S3Client s3Client;
	@Value("${spring.aws.s3.bucket-name}")
	private String bucketName;
	@Value("${spring.aws.s3.max-file-size}")
	private long maxFileSize;
	@Value("${spring.aws.s3.allowed-extensions}")
	private String allowedExtensions;

	/**
	 * 이미지를 검증하고 S3에 업로드한 뒤 업로드된 객체의 S3 키를 반환한다.
	 *
	 * @param request 업로드할 MultipartFile과 userId, 선택적 postId를 포함한 요청 객체. file은 비어있지 않아야 하며 파일 크기와 확장자는 서비스 설정에 따라 검증된다.
	 * @return 업로드된 객체의 S3 키를 포함한 ImageUploadResponse
	 * @throws HelloPetException S3 클라이언트 관련 오류가 발생하면 `HelloPetExceptionCode.FILE_PROCESS_ERROR`로, 그 외 예기치 않은 오류는 `HelloPetExceptionCode.INTERNAL_SERVER_ERROR`로 발생한다.
	 */
	@Override
	public ImageUploadResponse uploadImage(ImageUploadRequest request) {
		validateFile(request.file());
		String s3Key = generateS3Key(request);
		try {
			uploadFileToS3(request.file(), s3Key);
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

	/**
	 * 주어진 MultipartFile을 지정된 S3 키(s3Key)로 업로드한다.
	 *
	 * 업로드 시 원본 파일명("original-filename")과 업로드 시각("upload-timestamp")을 메타데이터로 함께 저장한다.
	 *
	 * @param file  업로드할 파일
	 * @param s3Key S3에 저장할 객체 키(경로 포함)
	 * @throws HelloPetException 파일 스트림 처리 중 오류가 발생하면 `FILE_PROCESS_ERROR` 코드로 발생시킨다
	 */
	private void uploadFileToS3(MultipartFile file, String s3Key) {
		try (InputStream is = file.getInputStream()) {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(s3Key)
				.contentType(file.getContentType())
				.contentLength(file.getSize())
				.metadata(Map.of(
					"original-filename", file.getOriginalFilename(),
					"upload-timestamp", String.valueOf(System.currentTimeMillis())
				))
				.build();

			s3Client.putObject(putObjectRequest,
				RequestBody.fromInputStream(is, file.getSize()));
		} catch (IOException e) {
			throw new HelloPetException(HelloPetExceptionCode.FILE_PROCESS_ERROR);
		}
	}

	/**
	 * S3에 저장할 객체 키(경로와 파일명)를 생성합니다.
	 *
	 * @param request 업로드 요청 객체 — 키는 request.userId()를 최상위 경로로 사용하고, request.postId()가 있으면 그 하위 폴더에 배치하며 업로드 파일의 원본 파일명에서 추출한 확장자를 사용합니다. 파일명은 타임스탬프와 짧은 UUID 접미사로 구성됩니다.
	 * @return 생성된 S3 객체 키. 예: "userId/imageName.ext" 또는 "userId/postId/imageName.ext"
	 */
	private String generateS3Key(ImageUploadRequest request) {
		String imageName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5);
		String imageExtension = getFileExtension(request.file().getOriginalFilename());
		if (request.postId() == null) {
			return String.format("%s/%s.%s", request.userId(), imageName, imageExtension);
		} else {
			return String.format("%s/%s/%s.%s", request.userId(), request.postId(), imageName, imageExtension);
		}
	}

	/**
	 * 업로드할 MultipartFile의 유효성을 검사한다.
	 *
	 * @param file 검사할 멀티파트 파일
	 * @throws HelloPetException `FILE_IS_EMPTY` 파일이 null이거나 비어있을 때
	 * @throws HelloPetException `FILE_SIZE_BIG` 파일 크기가 허용 최대값을 초과할 때
	 * @throws HelloPetException `FILE_NAME_IS_EMPTY` 원본 파일명이 없거나 빈 문자열일 때
	 * @throws HelloPetException `FILE_EXTENSION_NOT_ALLOW` 파일 확장자가 허용 목록에 포함되지 않을 때
	 */
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
		String extension = getFileExtension(fileName).toLowerCase();
		List<String> allowedExtensionList = Arrays.asList(allowedExtensions.split(","));

		if (!allowedExtensionList.contains(extension)) {
			throw new HelloPetException(HelloPetExceptionCode.FILE_EXTENSION_NOT_ALLOW);
		}
	}

	/**
	 * 파일 이름에서 확장자를 추출한다.
	 *
	 * @param fileName 확장자를 포함할 수 있는 파일 이름
	 * @return 파일 확장자(마지막 점('.') 이후 부분). 확장자가 없으면 빈 문자열을 반환한다.
	 */
	private String getFileExtension(String fileName) {
		int lastDotIndex = fileName.lastIndexOf('.');
		return (lastDotIndex >= 0) ? fileName.substring(lastDotIndex + 1) : "";
	}
}
