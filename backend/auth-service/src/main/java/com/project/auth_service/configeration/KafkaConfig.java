package com.project.auth_service.configeration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic createMyTopic(){
        return new NewTopic("AuthenicationNotification" , 1 , (short) 1);
    }
}
