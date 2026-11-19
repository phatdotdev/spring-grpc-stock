package com.doph.protobuff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProtobuffApplication {

	public static void main(String[] args) {

//		com.doph.protobuff.Employee.newBuilder().setId(101).setName("Do Phat");
		SpringApplication.run(ProtobuffApplication.class, args
		);
	}

}
