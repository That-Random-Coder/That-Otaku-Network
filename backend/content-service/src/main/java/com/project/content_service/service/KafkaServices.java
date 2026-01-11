package com.project.content_service.service;

import com.project.content_service.domain.dto.KafkaDto;
import com.project.content_service.domain.enums.KafkaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaServices {

    private final KafkaTemplate<String, KafkaDto> kafkaTemplate;
    private static final String CONTENT_TOPIC = "ContentRecommendation";

    public void sendContentEvent(KafkaType type, Map<String, Object> payload) {

        KafkaDto dto = KafkaDto.builder()
                .TopicType(type.name())
                .map(payload)
                .build();
        try {
            kafkaTemplate.send(CONTENT_TOPIC, dto);
            log.info("Kafka event sent: {} with data {}", type, payload);
        } catch (Exception e) {
            log.error("Failed to send Kafka event: {} with data {}", type, payload);
            log.error(e.getMessage());
        }
    }
}
