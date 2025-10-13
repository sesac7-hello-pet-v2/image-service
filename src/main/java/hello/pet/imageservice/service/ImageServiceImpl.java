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

	private String generateS3Key(ImageUploadRequest request) {
		String imageName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5);
		String imageExtension = getFileExtension(request.file().getOriginalFilename());
		if (request.postId() == null) {
			return String.format("%s/%s.%s", request.userId(), imageName, imageExtension);
		} else {
			return String.format("%s/%s/%s.%s", request.userId(), request.postId(), imageName, imageExtension);
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
}
