package com.dearwith.dearwith_backend;

import com.dearwith.dearwith_backend.external.apple.AppleAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(AppleAuthProperties.class)
public class DearwithBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DearwithBackendApplication.class, args);
	}

}
