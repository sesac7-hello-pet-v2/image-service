package hello.pet.imageservice.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

	private void uploadFileToS3(MultipartFile file, String s3Key) {
		try {
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
				RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
		} catch (IOException e) {
			throw new HelloPetException(HelloPetExceptionCode.FILE_PROCESS_ERROR);
		}
	}

	private String generateS3Key(ImageUploadRequest request) {
		Long imageName = System.currentTimeMillis();
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
