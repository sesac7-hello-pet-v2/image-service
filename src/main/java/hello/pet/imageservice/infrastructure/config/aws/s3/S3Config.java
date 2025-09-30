package hello.pet.imageservice.infrastructure.config.aws.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import hello.pet.imageservice.infrastructure.exception.HelloPetException;
import hello.pet.imageservice.infrastructure.exception.HelloPetExceptionCode;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {
	@Value("${spring.aws.s3.region}")
	private String region;

	@Value("${spring.aws.s3.access-key}")
	private String accessKey;

	@Value("${spring.aws.s3.secret-key}")
	private String secretKey;

	/**
	 * 지정된 AWS 리전과 구성된 자격 증명 공급자를 사용하여 S3Client 빈을 생성한다.
	 *
	 * @return 구성된 리전과 자격 증명 공급자를 사용하는 S3Client 인스턴스
	 */
	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(awsCredentialsProvider())
			.build();
	}

	/**
	 * AWS S3 클라이언트에 사용할 AwsCredentialsProvider를 제공한다.
	 *
	 * @return StaticCredentialsProvider 인스턴스 — `accessKey`와 `secretKey`가 설정되어 있을 때 AwsBasicCredentials로 구성된 자격 증명 제공자
	 * @throws HelloPetException `accessKey` 또는 `secretKey`가 없거나 빈 문자열일 경우, `HelloPetExceptionCode.S3_CONFIG_FAIL` 코드로 예외가 발생함
	 */
	private AwsCredentialsProvider awsCredentialsProvider() {
		// 환경변수나 설정파일에 access-key, secret-key가 있으면 사용
		// 없으면 DefaultCredentialsProvider 사용 (IAM Role, EC2 Instance Profile 등)
		if (accessKey != null && !accessKey.isEmpty()
			&& secretKey != null && !secretKey.isEmpty()) {
			return StaticCredentialsProvider.create(
				AwsBasicCredentials.create(accessKey, secretKey)
			);
		}
		throw new HelloPetException(HelloPetExceptionCode.S3_CONFIG_FAIL);
	}
}
