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
	 * 이미지 업로드 요청을 검증하고 파일을 S3에 저장한 뒤 저장된 S3 키를 포함한 응답을 반환합니다.
	 *
	 * @param request 업로드할 파일, 사용자 ID 및 선택적 포스트 ID를 포함하는 요청 객체
	 * @return 저장된 객체의 S3 키를 포함한 ImageUploadResponse
	 * @throws HelloPetException 요청 파일이 비어있거나 크기/확장자가 허용되지 않는 경우(FILE_IS_EMPTY, FILE_SIZE_BIG, FILE_NAME_IS_EMPTY, FILE_EXTENSION_NOT_ALLOW),
	 *                            S3 업로드 또는 파일 처리 중 오류가 발생한 경우(FILE_PROCESS_ERROR),
	 *                            그 외 예상치 못한 내부 오류가 발생한 경우(INTERNAL_SERVER_ERROR)
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
	 * 주어진 MultipartFile을 지정한 S3 키로 Amazon S3에 업로드한다.
	 *
	 * @param file  업로드할 파일
	 * @param s3Key S3에 저장할 객체 키
	 * @throws HelloPetException FILE_PROCESS_ERROR 코드로 감싼 파일 처리 오류가 발생하면 던져진다
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
	 * S3에 저장할 객체 키(경로 포함)를 생성한다.
	 *
	 * 요청의 userId와 파일 이름에서 추출한 확장자, 타임스탬프 기반 랜덤 식별자를 조합하여
	 * 다음 형식의 키를 반환한다.
	 * - postId가 없는 경우: `userId/imageName.extension`
	 * - postId가 있는 경우: `userId/postId/imageName.extension`
	 *
	 * @param request 파일의 원래 이름, userId 및 선택적 postId를 포함한 업로드 요청
	 * @return S3에 저장할 객체 키(예: `userId/postId/1633024800000_ab12c.jpg`)
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
	 * 업로드할 파일의 유효성을 검사하고 조건에 맞지 않으면 적절한 예외를 던진다.
	 *
	 * 검사 항목:
	 * - 파일이 null이거나 비어있는지
	 * - 파일 크기가 허용 최대치(maxFileSize)를 초과하는지
	 * - 원래 파일명이 비어있는지
	 * - 파일 확장자가 허용된 확장자 목록(allowedExtensions)에 포함되는지
	 *
	 * @param file 검사할 MultipartFile
	 * @throws HelloPetException 파일이 null이거나 비어있을 경우 {@code HelloPetExceptionCode.FILE_IS_EMPTY}
	 * @throws HelloPetException 파일 크기가 허용값을 초과할 경우 {@code HelloPetExceptionCode.FILE_SIZE_BIG}
	 * @throws HelloPetException 원래 파일명이 비어있을 경우 {@code HelloPetExceptionCode.FILE_NAME_IS_EMPTY}
	 * @throws HelloPetException 확장자가 허용 목록에 없을 경우 {@code HelloPetExceptionCode.FILE_EXTENSION_NOT_ALLOW}
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
	 * @param fileName 확장자를 추출할 파일 이름
	 * @return 파일 이름의 마지막 '.' 이후에 있는 확장자 문자열; 확장자가 없으면 빈 문자열
	 */
	private String getFileExtension(String fileName) {
		int lastDotIndex = fileName.lastIndexOf('.');
		return (lastDotIndex >= 0) ? fileName.substring(lastDotIndex + 1) : "";
	}
}
