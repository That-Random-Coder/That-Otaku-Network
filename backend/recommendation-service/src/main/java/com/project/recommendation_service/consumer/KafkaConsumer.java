package com.project.recommendation_service.consumer;

import com.project.recommendation_service.domain.dto.KafkaDto;
import com.project.recommendation_service.domain.enums.KafkaType;
import com.project.recommendation_service.service.KafkaService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class KafkaConsumer {

    private final KafkaService kafkaService;

    @KafkaListener(topics = "ContentRecommendation", groupId = "recommendation")
    public void kafkaConsuming(KafkaDto dto) {
        String type = dto.getTopicType();
        try {
            KafkaType kafkaType = KafkaType.valueOf(type); // safer enum handling
            switch (kafkaType) {
                case CREATE -> kafkaService.createContent(dto.getMap());
                case DELETE -> kafkaService.deleteContent(dto.getMap());
                case DISABLE -> kafkaService.disableContent(dto.getMap());
                case ENABLE -> kafkaService.enableContent(dto.getMap());
                case LIKE -> kafkaService.likeContent(dto.getMap());
                case DISLIKE -> kafkaService.dislikeContent(dto.getMap());
                case SHARE -> kafkaService.shareContent(dto.getMap());
                case COMMENT -> kafkaService.commentContent(dto.getMap());
                case REMOVE_LIKE -> kafkaService.removeLikeContent(dto.getMap());
                case REMOVE_DISLIKE -> kafkaService.removeDisLikeContent(dto.getMap());
                case CHANGE_TO_LIKE -> kafkaService.changeDisLikeContent(dto.getMap());
                case CHANGE_TO_DISLIKE -> kafkaService.changeLikeContent(dto.getMap());
                default -> log.warn("Unhandled KafkaType: {}", type);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid KafkaType received: {}", type, e);
        } catch (Exception e) {
            log.error("Error processing Kafka event: {}", type, e);
        }
    }
}

