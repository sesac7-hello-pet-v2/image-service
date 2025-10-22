package hello.pet.imageservice.infrastructure.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "imageResizeExecutor")
	public Executor imageResizeExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);           // 기본 스레드 수
		executor.setMaxPoolSize(5);            // 최대 스레드 수
		executor.setQueueCapacity(100);        // 대기 큐 크기
		executor.setThreadNamePrefix("ImageResize-");
		executor.initialize();
		return executor;
	}
}
