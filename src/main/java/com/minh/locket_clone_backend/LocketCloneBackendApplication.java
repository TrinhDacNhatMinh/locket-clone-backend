package com.minh.locket_clone_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class LocketCloneBackendApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		SpringApplication.run(LocketCloneBackendApplication.class, args);
	}

}
