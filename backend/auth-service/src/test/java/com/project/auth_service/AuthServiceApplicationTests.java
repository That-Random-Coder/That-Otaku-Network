package com.project.auth_service;

import com.project.auth_service.domain.enums.Roles;
import com.project.auth_service.domain.enums.TokenType;
import com.project.auth_service.service.JwtService;
import io.jsonwebtoken.lang.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

@ActiveProfiles("test")
@SpringBootTest
class AuthServiceApplicationTests {

	private String token = "AVx9KQp7mT3ZrE2Wn8YFJHcL5B0uD4aS6GkPqR1oI";

	@Autowired
	private JwtService jwtService;

    @Test
	void contextLoads() {

		UUID id = UUID.randomUUID();
		System.out.println(id.toString());
		Long expire = 2592000000L;
		String jwt = jwtService.generateToken(id , Roles.USER.toString() , expire  , TokenType.ACCESS.toString());

		System.out.println(jwt);
		Assert.notNull(jwt);

	}

}
