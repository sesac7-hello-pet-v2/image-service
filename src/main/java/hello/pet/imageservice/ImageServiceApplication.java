package hello.pet.imageservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ImageServiceApplication {

	/**
	 * 애플리케이션을 시작하여 Spring 애플리케이션 컨텍스트를 초기화한다.
	 *
	 * @param args 애플리케이션에 전달된 명령줄 인수
	 */
	public static void main(String[] args) {
		SpringApplication.run(ImageServiceApplication.class, args);
	}

}
