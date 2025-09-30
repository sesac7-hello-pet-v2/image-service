package hello.pet.imageservice.infrastructure.config.mongo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * MongoDB 관련 설정을 정의한 클래스입니다.
 * 이 클래스는 MongoRepository에서 구현이 어려운 커스텀 CRUD 기능을 관리하고,
 * MongoDB에 대한 다양한 설정을 제공합니다.
 *
 * <p>주요 기능으로는 MongoDB 템플릿을 정의하고, `_class` 필드 비활성화 및
 * MongoDB에서 유효성 검사를 위한 설정을 포함합니다.</p>
 *
 * <p>이 클래스는 {@link Configuration} 애노테이션을 통해 Spring 애플리케이션에 등록되며,
 * {@link EnableMongoAuditing}을 통해 MongoDB 엔티티의 생성 및 수정 시간을 자동으로 관리하는
 * 기능을 활성화합니다.</p>
 *
 * @since 2024-07-30
 * @version 1.0
 * @author namung08
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {

	/**
	 * MongoDB 작업을 수행할 수 있는 {@link MongoTemplate} 빈을 정의합니다.
	 * 이 템플릿은 MongoDB의 데이터베이스와 상호작용하며, `_class` 필드를 비활성화하여
	 * MongoDB에 엔티티의 클래스 정보를 저장하지 않도록 설정합니다.
	 *
	 * @param mongoDatabaseFactory MongoDB 데이터베이스 팩토리
	 * @param converter MongoDB 변환기 (엔티티와 MongoDB 간의 데이터 변환을 처리)
	 * @return MongoDB와의 상호작용을 위한 {@link MongoTemplate} 객체
	 */
	@Bean
	public MongoTemplate mongoTemplate(
		MongoDatabaseFactory mongoDatabaseFactory,
		MappingMongoConverter converter
	) {
		// MongoDB에 `_class` 필드가 저장되지 않도록 설정
		converter.setTypeMapper(new DefaultMongoTypeMapper(null));
		return new MongoTemplate(mongoDatabaseFactory, converter);
	}

	/**
	 * MongoDB 엔티티에 대해 저장 및 업데이트 시 Bean Validation을 수행하는 ValidatingMongoEventListener 빈을 생성한다.
	 *
	 * @return MongoDB 엔티티의 제약 조건을 검증하는 {@link ValidatingMongoEventListener}
	 */
	@Bean
	public ValidatingMongoEventListener validatingMongoEventListener() {
		return new ValidatingMongoEventListener(validator().getValidator());
	}

	/**
	 * JSR-303/JSR-380 Bean Validation을 위한 LocalValidatorFactoryBean 빈을 생성합니다.
	 *
	 * @return JSR-303/JSR-380 Validator를 제공하는 LocalValidatorFactoryBean 인스턴스
	 */
	@Bean
	public LocalValidatorFactoryBean validator() {
		return new LocalValidatorFactoryBean();
	}
}
