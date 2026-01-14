package com.binder.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.binder.demo")
public class BinderApplication {

	public static void main(String[] args) {
		SpringApplication.run(BinderApplication.class, args);
	}

}
