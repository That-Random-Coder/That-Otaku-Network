package com.project.search_service.consumer;

import com.project.search_service.domain.dto.kafka.DataTransfer;
import com.project.search_service.domain.enums.KafkaDataTransferFields;
import com.project.search_service.service.KafkaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConsumers {

    private final KafkaService kafkaService;

    public KafkaConsumers(KafkaService kafkaService) {
        this.kafkaService = kafkaService;
    }

    @KafkaListener(topics = "SaveUserDatabase")
    public void consumers(DataTransfer transfer) {
        if (transfer.getType().equals(KafkaDataTransferFields.USER_SAVE.toString())) {
            kafkaService.createUser(transfer);
        } else if (transfer.getType().equals(KafkaDataTransferFields.USER_UPDATE.toString())) {
            kafkaService.updateUser(transfer);
        } else if (transfer.getType().equals(KafkaDataTransferFields.GROUP_SAVE.toString())) {
            kafkaService.saveGroup(transfer);
        }else {
            log.info("The Kafka Field is Undefined with type : {}", transfer.getType());
        }
    }

}
