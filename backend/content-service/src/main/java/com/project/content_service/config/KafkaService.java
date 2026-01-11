package com.project.content_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaService {

    @Bean
    public NewTopic topics(){
        return new NewTopic("ContentRecommendation" , 1, (short) 1);
    }

}
