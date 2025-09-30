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
	 * 애플리케이션에서 사용할 AWS S3 클라이언트를 구성하여 반환한다.
	 *
	 * <p>환경에 설정된 region 값을 검증하며, 값이 없거나 빈 문자열일 경우 S3 구성 실패 예외를 발생시킨다.</p>
	 *
	 * @return 구성된 S3Client 인스턴스
	 * @throws HelloPetException region 설정이 없거나 비어 있을 경우 발생하며, 예외 코드 `S3_CONFIG_FAIL`를 사용한다.
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
	 * 구성된 액세스 키와 시크릿 키가 있으면 해당 자격 증명을 사용한 정적 자격 공급자를,
	 * 없으면 기본 자격 공급자(DefaultCredentialsProvider)를 반환한다.
	 *
	 * @return 액세스 키와 시크릿 키가 모두 설정된 경우 StaticCredentialsProvider(해당 키로 생성된 AwsBasicCredentials),
	 *         그렇지 않은 경우 DefaultCredentialsProvider
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
