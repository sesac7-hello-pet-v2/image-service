package hello.pet.imageservice.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import hello.pet.imageservice.infrastructure.utils.ImageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncImageService {

	private final S3Client s3Client;
	private final ImageUtils imageUtils = new ImageUtils();

	@Value("${spring.aws.s3.bucket-name}")
	private String bucketName;

	@Async("imageResizeExecutor")
	public CompletableFuture<Void> resizeAndUploadAsync(
		byte[] originalImageBytes,
		String originalS3Key,
		String contentType) {
		try {
			log.info("Starting async image resize for: {}", originalS3Key);

			// Feed용 리사이징 (600x600)
			try (InputStream feedInputStream = new java.io.ByteArrayInputStream(originalImageBytes)) {
				BufferedImage feedImage = imageUtils.getResizingImage(feedInputStream, ImageUtils.ImageSize.FEED);
				String feedS3Key = generateResizedKey(originalS3Key, "feed");
				uploadResizedImage(feedImage, feedS3Key, contentType);
			}

			// Thumbnail용 리사이징 (300x300)
			try (InputStream thumbnailInputStream = new java.io.ByteArrayInputStream(originalImageBytes)) {
				BufferedImage thumbnailImage = imageUtils.getResizingImage(thumbnailInputStream,
					ImageUtils.ImageSize.THUMBNAIL);
				String thumbnailS3Key = generateResizedKey(originalS3Key, "thumb");
				uploadResizedImage(thumbnailImage, thumbnailS3Key, contentType);
			}

			log.info("Completed async image resize for: {}", originalS3Key);
			return CompletableFuture.completedFuture(null);

		} catch (Exception e) {
			log.error("Failed to resize and upload image: {}", originalS3Key, e);
			return CompletableFuture.failedFuture(e);
		}
	}

	private void uploadResizedImage(BufferedImage image, String s3Key, String contentType) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		String format = getImageFormat(contentType);
		ImageIO.write(image, format, outputStream);
		byte[] imageBytes = outputStream.toByteArray();

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(s3Key)
			.contentType(contentType)
			.contentLength((long)imageBytes.length)
			.metadata(Map.of(
				"resized", "true",
				"upload-timestamp", String.valueOf(System.currentTimeMillis())
			))
			.build();

		s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));
		log.info("Uploaded resized image: {}", s3Key);
	}

	private String generateResizedKey(String originalKey, String sizeSuffix) {
		// original: userId/postId/filename.jpg
		// result: userId/postId/filename_feed.jpg or userId/postId/filename_thumb.jpg
		int lastDotIndex = originalKey.lastIndexOf('.');
		if (lastDotIndex > 0) {
			String baseName = originalKey.substring(0, lastDotIndex);
			String extension = originalKey.substring(lastDotIndex);
			return baseName + "_" + sizeSuffix + extension;
		}
		return originalKey + "_" + sizeSuffix;
	}

	private String getImageFormat(String contentType) {
		if (contentType != null) {
			if (contentType.contains("png"))
				return "png";
			if (contentType.contains("gif"))
				return "gif";
			if (contentType.contains("webp"))
				return "webp";
		}
		return "jpg"; // 기본값
	}
}
