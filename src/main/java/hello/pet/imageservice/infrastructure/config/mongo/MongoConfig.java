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
	 * MongoDB와 상호작용하는 빈을 구성한다.
	 *
	 * MongoDB에 엔티티의 클래스 정보를 `_class` 필드로 저장하지 않도록 타입 매퍼를 비활성화한 상태로
	 * MongoDB 연산에 사용할 템플릿을 반환한다.
	 *
	 * @param mongoDatabaseFactory MongoDB 데이터베이스 팩토리
	 * @param converter MongoDB와 도메인 객체 간 변환을 처리하는 매퍼
	 * @return MongoDB와의 상호작용에 사용되는 MongoTemplate 인스턴스
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
	 * 스프링의 {@link LocalValidatorFactoryBean} 빈을 정의합니다.
	 * 이 빈은 JSR-303/JSR-380 Bean Validation을 위한 Validator를 제공합니다.
	 *
	 * @return {@link LocalValidatorFactoryBean} 유효성 검사 빈
	 */
	@Bean
	public LocalValidatorFactoryBean validator() {
		return new LocalValidatorFactoryBean();
	}
}
