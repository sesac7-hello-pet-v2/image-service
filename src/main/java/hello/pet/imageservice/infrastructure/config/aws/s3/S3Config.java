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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {
	@Value("${spring.aws.s3.region}")
	private String region;

	@Value("${spring.aws.s3.access-key}")
	private String accessKey;

	@Value("${spring.aws.s3.secret-key}")
	private String secretKey;

	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(awsCredentialsProvider())
			.build();
	}

	@Bean
	public S3Presigner s3Presigner() {
		return S3Presigner.builder()
			.region(Region.of(region))
			.credentialsProvider(awsCredentialsProvider())
			.build();
	}

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
