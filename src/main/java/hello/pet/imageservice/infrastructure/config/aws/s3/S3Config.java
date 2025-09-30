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
	 * 애플리케이션에서 사용할 AWS S3 클라이언트 빈을 구성하여 제공한다.
	 *
	 * 구성에는 설정된 리전과 자격 증명 공급자가 적용된다.
	 *
	 * @return 구성된 S3Client 인스턴스
	 */
	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(awsCredentialsProvider())
			.build();
	}

	/**
	 * 애플리케이션 설정에 따라 AWS 자격증명 공급자를 생성하여 반환한다.
	 *
	 * @return 액세스 키와 시크릿 키가 설정되어 있으면 해당 값으로 생성한 `StaticCredentialsProvider`를 반환하는 `AwsCredentialsProvider`
	 * @throws HelloPetException S3_CONFIG_FAIL 액세스 키 또는 시크릿 키가 설정되어 있지 않은 경우 발생
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
