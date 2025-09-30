package hello.pet.imageservice.infrastructure.config.aws.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import hello.pet.imageservice.infrastructure.exception.HelloPetException;
import hello.pet.imageservice.infrastructure.exception.HelloPetExceptionCode;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
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
	 * 구성된 AWS S3 클라이언트(S3Client)를 생성한다.
	 *
	 * 설정된 region 값을 사용하여 S3Client를 구성하고 반환한다.
	 *
	 * @return 구성된 S3Client 인스턴스
	 * @throws HelloPetException region이 null이거나 빈 문자열인 경우 HelloPetExceptionCode.S3_CONFIG_FAIL 코드로 발생
	 */
	@Bean
	public S3Client s3Client() {
		if (region == null || region.isEmpty()) {
			throw new HelloPetException(HelloPetExceptionCode.S3_CONFIG_FAIL);
		}
		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(awsCredentialsProvider())
			.build();
	}

	/**
	 * 환경에 따라 적절한 AWS 자격증명 공급자를 반환한다.
	 *
	 * <p>accessKey와 secretKey가 둘 다 설정되어 있으면 해당 키로 동작하는 StaticCredentialsProvider를 반환하고,
	 * 그렇지 않으면 DefaultCredentialsProvider를 반환한다.
	 *
	 * @return AwsCredentialsProvider 인스턴스 — accessKey/secretKey가 설정되어 있으면 해당 키를 사용하는 StaticCredentialsProvider, 그렇지 않으면 DefaultCredentialsProvider
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
		return DefaultCredentialsProvider.create();
	}
}
