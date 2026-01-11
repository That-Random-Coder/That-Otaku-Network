package com.project.user_service;

import com.project.user_service.repository.UsersRepository;
import com.project.user_service.service.KafkaService;
import com.project.user_service.service.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

@DisplayName("User-Service-Test")
@ExtendWith(MockitoExtension.class)
class UserServiceApplicationTests {

	@Mock
	private UsersRepository usersRepository;

	@Mock
	private RedisService redisService;

	@Mock
	private KafkaService kafkaService;

	@Nested
	@DisplayName("Create-User")
	class CreateUser{



	}

}
