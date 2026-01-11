package com.project.search_service.consumer;

import com.project.search_service.domain.dto.kafka.DataTransfer;
import com.project.search_service.domain.enums.KafkaDataTransferFields;
import com.project.search_service.service.KafkaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaConsumers Tests")
class KafkaConsumersTest {

    @Mock
    private KafkaService kafkaService;

    @InjectMocks
    private KafkaConsumers kafkaConsumers;

    private DataTransfer testTransfer;
    private Map<String, String> testData;

    @BeforeEach
    void setUp() {
        testData = new HashMap<>();
        testData.put("ID", "1");
        testData.put("USERNAME", "testuser");
        testData.put("DISPLAY_NAME", "Test User");
        testData.put("BIO", "Test bio");

        testTransfer = DataTransfer.builder()
                .type(KafkaDataTransferFields.USER_SAVE.toString())
                .map(testData)
                .build();
    }

    @Nested
    @DisplayName("When consuming Kafka messages")
    class ConsumeMessageTests {

        @Test
        @DisplayName("Should call createUser when transfer type is USER_SAVE")
        void testConsumeUserSaveMessage() {
            testTransfer.setType(KafkaDataTransferFields.USER_SAVE.toString());

            kafkaConsumers.consumers(testTransfer);

            verify(kafkaService, times(1)).createUser(testTransfer);
            verify(kafkaService, never()).updateUser(any());
        }

        @Test
        @DisplayName("Should call updateUser when transfer type is USER_UPDATE")
        void testConsumeUserUpdateMessage() {
            testTransfer.setType(KafkaDataTransferFields.USER_UPDATE.toString());

            kafkaConsumers.consumers(testTransfer);

            verify(kafkaService, times(1)).updateUser(testTransfer);
            verify(kafkaService, never()).createUser(any());
        }

        @Test
        @DisplayName("Should handle undefined Kafka field types gracefully")
        void testConsumeUndefinedMessage() {
            testTransfer.setType("UNDEFINED_TYPE");

            kafkaConsumers.consumers(testTransfer);

            verify(kafkaService, never()).createUser(any());
            verify(kafkaService, never()).updateUser(any());
        }

        @Test
        @DisplayName("Should process valid USER_SAVE transfer with all data")
        void testProcessUserSaveWithAllData() {
            testTransfer.setType(KafkaDataTransferFields.USER_SAVE.toString());

            kafkaConsumers.consumers(testTransfer);

            verify(kafkaService).createUser(testTransfer);
        }

        @Test
        @DisplayName("Should process valid USER_UPDATE transfer with all data")
        void testProcessUserUpdateWithAllData() {
            testTransfer.setType(KafkaDataTransferFields.USER_UPDATE.toString());

            kafkaConsumers.consumers(testTransfer);

            verify(kafkaService).updateUser(testTransfer);
        }

        @Test
        @DisplayName("Should handle null data map")
        void testConsumeWithNullDataMap() {
            testTransfer.setMap(null);
            testTransfer.setType(KafkaDataTransferFields.USER_SAVE.toString());

            kafkaConsumers.consumers(testTransfer);

            verify(kafkaService).createUser(testTransfer);
        }

        @Test
        @DisplayName("Should handle empty data map")
        void testConsumeWithEmptyDataMap() {
            testTransfer.setMap(new HashMap<>());
            testTransfer.setType(KafkaDataTransferFields.USER_SAVE.toString());

            kafkaConsumers.consumers(testTransfer);

            verify(kafkaService).createUser(testTransfer);
        }
    }

    @Nested
    @DisplayName("When handling different transfer types")
    class TransferTypeTests {

        @Test
        @DisplayName("Should recognize USER_SAVE transfer type")
        void testUserSaveType() {
            testTransfer.setType(KafkaDataTransferFields.USER_SAVE.toString());

            kafkaConsumers.consumers(testTransfer);

            verify(kafkaService).createUser(testTransfer);
        }

        @Test
        @DisplayName("Should recognize USER_UPDATE transfer type")
        void testUserUpdateType() {
            testTransfer.setType(KafkaDataTransferFields.USER_UPDATE.toString());

            kafkaConsumers.consumers(testTransfer);

            verify(kafkaService).updateUser(testTransfer);
        }

        @Test
        @DisplayName("Should not process invalid transfer types")
        void testInvalidType() {
            testTransfer.setType("INVALID_TYPE");

            kafkaConsumers.consumers(testTransfer);

            verify(kafkaService, never()).createUser(any());
            verify(kafkaService, never()).updateUser(any());
        }

        @Test
        @DisplayName("Should be case-sensitive for transfer types")
        void testCaseSensitiveType() {
            testTransfer.setType("user_save");

            kafkaConsumers.consumers(testTransfer);

            verify(kafkaService, never()).createUser(any());
            verify(kafkaService, never()).updateUser(any());
        }

        @Test
        @DisplayName("Should handle whitespace in transfer type")
        void testWhitespaceInType() {
            testTransfer.setType(" " + KafkaDataTransferFields.USER_SAVE.toString() + " ");

            kafkaConsumers.consumers(testTransfer);

            verify(kafkaService, never()).createUser(any());
        }
    }

    @Nested
    @DisplayName("When processing multiple transfers")
    class MultipleTransfersTests {

        @Test
        @DisplayName("Should handle consecutive USER_SAVE messages")
        void testConsecutiveUserSaveMessages() {
            DataTransfer transfer1 = DataTransfer.builder()
                    .type(KafkaDataTransferFields.USER_SAVE.toString())
                    .map(testData)
                    .build();

            DataTransfer transfer2 = DataTransfer.builder()
                    .type(KafkaDataTransferFields.USER_SAVE.toString())
                    .map(testData)
                    .build();

            kafkaConsumers.consumers(transfer1);
            kafkaConsumers.consumers(transfer2);

            verify(kafkaService, times(2)).createUser(any());
        }

        @Test
        @DisplayName("Should handle mixed transfer types")
        void testMixedTransferTypes() {
            DataTransfer userSave = DataTransfer.builder()
                    .type(KafkaDataTransferFields.USER_SAVE.toString())
                    .map(testData)
                    .build();

            DataTransfer userUpdate = DataTransfer.builder()
                    .type(KafkaDataTransferFields.USER_UPDATE.toString())
                    .map(testData)
                    .build();

            kafkaConsumers.consumers(userSave);
            kafkaConsumers.consumers(userUpdate);

            verify(kafkaService).createUser(userSave);
            verify(kafkaService).updateUser(userUpdate);
        }

        @Test
        @DisplayName("Should handle undefined type after valid type")
        void testUndefinedTypeAfterValidType() {
            DataTransfer validTransfer = DataTransfer.builder()
                    .type(KafkaDataTransferFields.USER_SAVE.toString())
                    .map(testData)
                    .build();

            DataTransfer undefinedTransfer = DataTransfer.builder()
                    .type("UNDEFINED")
                    .map(testData)
                    .build();

            kafkaConsumers.consumers(validTransfer);
            kafkaConsumers.consumers(undefinedTransfer);

            verify(kafkaService).createUser(validTransfer);
            verify(kafkaService, never()).updateUser(any());
        }
    }
}
