package com.project.auth_service.service;

import com.project.auth_service.domain.dtos.NotificationDto;
import com.project.auth_service.domain.enums.EventTypeNotification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationService {

    private final KafkaTemplate kafkaTemplate;

    public void createEmailVerificationNotification(String email , String username , String message , EventTypeNotification type){
        String topic = "AuthenicationNotification";
        NotificationDto requestDto = NotificationDto
                .builder()
                .type(type.toString())
                .email(email)
                .username(username)
                .information(message)
                .build();
        try {
            kafkaTemplate.send(topic , requestDto);
            log.info("Verification Email is Send {} on {}" , username ,email);
        }catch (Exception e){
            log.error(e.getMessage());
        }

    }
}
