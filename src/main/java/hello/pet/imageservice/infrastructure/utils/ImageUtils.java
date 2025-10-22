package hello.pet.imageservice.infrastructure.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class ImageUtils {

	public BufferedImage getResizingImage(InputStream image, ImageSize targetSize) {
		try {
			// InputStream을 복사하여 EXIF 읽기와 이미지 로드를 각각 처리
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			image.transferTo(baos);
			InputStream exifInputStream = new ByteArrayInputStream(
				baos.toByteArray()
			);
			InputStream imageInputStream = new ByteArrayInputStream(
				baos.toByteArray()
			);

			// EXIF 데이터에서 'Orientation' 태그 값 추출
			int orientation = getOrientationFromExif(exifInputStream);

			// 이미지 로드
			BufferedImage bufferedImage = ImageIO.read(imageInputStream);

			// 이미지 회전
			bufferedImage = rotateImageIfRequired(bufferedImage, orientation);

			// 정사각형으로 크롭 후 리사이징
			return resizeToSquare(bufferedImage, targetSize.getSize());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// 기존 메서드 호환성을 위해 유지 (기본값: FEED 크기)
	public BufferedImage getResizingImage(InputStream image) {
		return getResizingImage(image, ImageSize.FEED);
	}

	private BufferedImage resizeToSquare(BufferedImage image, int targetSize) {
		// 정사각형으로 크롭
		BufferedImage croppedImage = cropToSquare(image);

		// 타겟 크기로 리사이징
		return Scalr.resize(
			croppedImage,
			Scalr.Method.QUALITY,
			Scalr.Mode.FIT_EXACT,
			targetSize,
			targetSize,
			Scalr.OP_ANTIALIAS
		);
	}

	private BufferedImage cropToSquare(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();

		if (width == height) {
			return image; // 이미 정사각형
		}

		// 더 작은 쪽을 기준으로 정사각형으로 크롭
		int cropSize = Math.min(width, height);
		int x = (width - cropSize) / 2;
		int y = (height - cropSize) / 2;

		return Scalr.crop(image, x, y, cropSize, cropSize);
	}

	private int getOrientationFromExif(InputStream image) {
		try {
			// EXIF 데이터에서 'Orientation' 태그 값 추출
			Metadata metadata = ImageMetadataReader.readMetadata(image);
			for (Directory directory : metadata.getDirectories()) {
				if (directory.containsTag(ExifSubIFDDirectory.TAG_ORIENTATION)) {
					return directory.getInt(ExifSubIFDDirectory.TAG_ORIENTATION);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 1; // Orientation 정보를 찾을 수 없으면 기본값(회전 없음)으로 설정
	}

	private BufferedImage rotateImageIfRequired(
		BufferedImage img,
		int orientation
	) {
		if (orientation == 6) {
			// 시계 방향 90도 회전
			return Scalr.rotate(img, Scalr.Rotation.CW_90);
		} else if (orientation == 3) {
			// 180도 회전
			return Scalr.rotate(img, Scalr.Rotation.CW_180);
		} else if (orientation == 8) {
			// 반시계 방향 90도 회전
			return Scalr.rotate(img, Scalr.Rotation.CW_270);
		}
		return img; // 회전이 필요 없는 경우
	}

	public enum ImageSize {
		FEED(600),      // Feed용 정사각형 이미지
		THUMBNAIL(300); // Board 썸네일용 작은 이미지

		private final int size;

		ImageSize(int size) {
			this.size = size;
		}

		public int getSize() {
			return size;
		}
	}
}
