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
	 * 업로드 요청의 파일을 검증하고 S3에 업로드한 뒤 업로드된 객체의 S3 키를 담은 응답을 생성한다.
	 *
	 * @param request 업로드할 파일과 업로더 정보(userId, 선택적 postId)를 포함한 요청 객체
	 * @return 업로드된 객체의 S3 키를 가진 ImageUploadResponse
	 * @throws HelloPetException 파일 처리 또는 S3 업로드 실패 시 HelloPetException(HelloPetExceptionCode.FILE_PROCESS_ERROR)
	 * @throws HelloPetException 예기치 않은 오류 발생 시 HelloPetException(HelloPetExceptionCode.INTERNAL_SERVER_ERROR)
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
	 * 지정된 S3 키로 MultipartFile을 업로드하고 객체 메타데이터를 설정한다.
	 *
	 * 업로드된 객체에는 원래 파일명(`original-filename`)과 업로드 시각(`upload-timestamp`) 메타데이터가 추가된다.
	 *
	 * @param file   업로드할 파일
	 * @param s3Key  S3에 저장할 객체 키(예: "userId/postId/123456789.jpg")
	 * @throws HelloPetException 파일 읽기 또는 처리 중 I/O 오류가 발생하면 `FILE_PROCESS_ERROR` 코드로 예외를 던진다.
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
	 * userId와 선택적 postId를 사용해 S3에 저장할 객체 키를 생성한다.
	 *
	 * @param request 이미지 파일과 userId, 선택적 postId를 포함하는 요청으로,
	 *                파일의 원래 이름에서 확장자를 추출해 키에 사용한다.
	 * @return S3에 저장할 객체 키 문자열. 형식은 "userId/timestamp.extension" 또는
	 *         "userId/postId/timestamp.extension"이다.
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
	 * <p>파일의 존재, 크기, 원래 파일명 및 확장자를 검증하고 조건을 만족하지 않으면 적절한 HelloPetException을 던진다.</p>
	 *
	 * @param file 검증할 업로드 파일
	 * @throws HelloPetException 파일이 비었을 때 (HelloPetExceptionCode.FILE_IS_EMPTY)
	 * @throws HelloPetException 파일 크기가 허용값을 초과할 때 (HelloPetExceptionCode.FILE_SIZE_BIG)
	 * @throws HelloPetException 파일명이 없거나 공백일 때 (HelloPetExceptionCode.FILE_NAME_IS_EMPTY)
	 * @throws HelloPetException 파일 확장자가 허용 목록에 없을 때 (HelloPetExceptionCode.FILE_EXTENSION_NOT_ALLOW)
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
	 * @param fileName 확장자를 포함한 파일 이름
	 * @return 파일 이름에 확장자가 있으면 마침표('.') 뒤의 확장자(점 없이), 없으면 빈 문자열
	 */
	private String getFileExtension(String fileName) {
		int lastDotIndex = fileName.lastIndexOf('.');
		return (lastDotIndex >= 0) ? fileName.substring(lastDotIndex + 1) : "";
	}
}
