package com.project.notification_service.consumer;

import com.project.notification_service.dto.NotificationDto;
import com.project.notification_service.enums.EventTypeNotification;
import com.project.notification_service.service.EmailValidationSender;
import lombok.AllArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class EmailValidationListoner {

    private final EmailValidationSender emailsender;

    @KafkaListener(topics = "AuthenicationNotification")
    public void EmailListoner(NotificationDto codeDto){
        if(codeDto.getType() == null || codeDto.getEmail() == null){
            return;
        }
        emailsender.sendEmail(codeDto);
    }

}
