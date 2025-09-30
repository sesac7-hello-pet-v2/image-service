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

	/**
	 * 이미지 파일을 검증하고 S3에 업로드한 뒤 업로드된 객체의 S3 키를 포함한 응답을 생성합니다.
	 *
	 * @param request 업로드할 파일과 업로더 정보를 포함하는 요청 객체 (파일, userId, 선택적 postId 등)
	 * @return 업로드된 객체의 S3 키를 포함하는 ImageUploadResponse
	 * @throws HelloPetException 파일 처리나 S3 업로드 오류가 발생한 경우 `FILE_PROCESS_ERROR`를 던집니다; 그 외의 예기치 않은 오류는 `INTERNAL_SERVER_ERROR`를 던집니다.
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
	 * 파일 내용을 S3에 지정한 객체 키로 업로드한다.
	 *
	 * 업로드 시 콘텐츠 타입·길이와 원본 파일명 및 업로드 타임스탬프를 메타데이터로 설정한다.
	 *
	 * @param file  업로드할 multipart 파일
	 * @param s3Key S3에 저장할 객체 키(경로 포함)
	 * @throws HelloPetException 파일의 스트림을 읽거나 처리하는 과정에서 오류가 발생하면 `FILE_PROCESS_ERROR` 코드로 래핑하여 던진다.
	 */
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

	/**
	 * userId와 선택적 postId, 업로드된 파일의 확장자 및 타임스탬프를 조합하여 S3에 저장할 객체 키를 생성합니다.
	 *
	 * @param request S3 키 생성을 위한 이미지 업로드 요청 — 내부적으로 userId, nullable한 postId, 그리고 파일의 원래 이름에서 확장자를 사용합니다.
	 * @return 생성된 S3 객체 키(형식 예: "userId/1627384950123.jpg" 또는 "userId/postId/1627384950123.png").
	 */
	private String generateS3Key(ImageUploadRequest request) {
		Long imageName = System.currentTimeMillis();
		String imageExtension = getFileExtension(request.file().getOriginalFilename());
		if (request.postId() == null) {
			return String.format("%s/%s.%s", request.userId(), imageName, imageExtension);
		} else {
			return String.format("%s/%s/%s.%s", request.userId(), request.postId(), imageName, imageExtension);
		}
	}

	/**
	 * 업로드된 파일의 유효성을 검사한다.
	 *
	 * 파일이 비어 있지 않고 허용된 크기 및 확장자를 가지며 원본 파일명이 존재하는지 확인한다.
	 *
	 * @param file 검사할 업로드 파일
	 * @throws HelloPetException FILE_IS_EMPTY when file is null or empty
	 * @throws HelloPetException FILE_SIZE_BIG when file size exceeds configured maxFileSize
	 * @throws HelloPetException FILE_NAME_IS_EMPTY when the original filename is null or blank
	 * @throws HelloPetException FILE_EXTENSION_NOT_ALLOW when the file extension is not in the allowed list
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
	 * 파일 이름에서 마지막 점('.') 이후의 확장자를 추출한다.
	 *
	 * @param fileName 확장자를 추출할 파일 이름
	 * @return 마침표를 제외한 확장자 문자열. 확장자가 없으면 빈 문자열을 반환한다.
	 */
	private String getFileExtension(String fileName) {
		int lastDotIndex = fileName.lastIndexOf('.');
		return (lastDotIndex >= 0) ? fileName.substring(lastDotIndex + 1) : "";
	}
}
