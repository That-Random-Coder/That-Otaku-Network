package com.project.user_service.service;

import com.project.user_service.domain.dto.kafka.DataTransfer;
import com.project.user_service.domain.entity.users.Users;
import com.project.user_service.domain.enums.KafkaDataTransferFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaService Tests")
class KafkaServiceTest {

    @Mock
    private KafkaTemplate<String, DataTransfer> kafkaTemplate;

    @InjectMocks
    private KafkaService kafkaService;

    private Users testUser;

    @BeforeEach
    void setUp() {

        testUser = new Users();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setBio("Test Bio");
    }

    @Nested
    @DisplayName("When saving user to Kafka")
    class SaveIntoUserDatabaseTests {

        @Test
        @DisplayName("Should send user data to SaveUserDatabase topic")
        void testSaveIntoUserDatabaseSuccess() {
            kafkaService.saveIntoUserDatabase(testUser);

            verify(kafkaTemplate, times(1)).send(
                    eq("SaveUserDatabase"),
                    ArgumentCaptor.forClass(DataTransfer.class).capture());
        }

        @Test
        @DisplayName("Should create DataTransfer with correct user information")
        void testDataTransferContainsCorrectData() {
            kafkaService.saveIntoUserDatabase(testUser);

            ArgumentCaptor<DataTransfer> dataTransferCaptor = ArgumentCaptor.forClass(DataTransfer.class);
            verify(kafkaTemplate).send(eq("SaveUserDatabase"), dataTransferCaptor.capture());

            DataTransfer sentData = dataTransferCaptor.getValue();

            assertThat(sentData.getType()).isEqualTo(KafkaDataTransferFields.USER_SAVE.toString());

            Map<String, String> dataMap = sentData.getMap();
            assertThat(dataMap).containsEntry(KafkaDataTransferFields.ID.toString(), testUser.getId().toString())
                    .containsEntry(KafkaDataTransferFields.USERNAME.toString(), "testuser")
                    .containsEntry(KafkaDataTransferFields.DISPLAY_NAME.toString(), "Test User")
                    .containsEntry(KafkaDataTransferFields.BIO.toString(), "Test Bio");
        }

        @Test
        @DisplayName("Should have correct number of fields in DataTransfer map")
        void testDataTransferMapHasAllFields() {
            kafkaService.saveIntoUserDatabase(testUser);

            ArgumentCaptor<DataTransfer> dataTransferCaptor = ArgumentCaptor.forClass(DataTransfer.class);
            verify(kafkaTemplate).send(eq("SaveUserDatabase"), dataTransferCaptor.capture());

            DataTransfer sentData = dataTransferCaptor.getValue();
            assertThat(sentData.getMap()).hasSize(4);
        }

        @Test
        @DisplayName("Should handle exception when Kafka send fails gracefully")
        void testHandleExceptionOnKafkaSendFailure() {
            doThrow(new RuntimeException("Kafka connection failed"))
                    .when(kafkaTemplate).send(anyString(), ArgumentCaptor.forClass(DataTransfer.class).capture());

            kafkaService.saveIntoUserDatabase(testUser);

            verify(kafkaTemplate, times(1)).send(anyString(), ArgumentCaptor.forClass(DataTransfer.class).capture());
        }

        @Test
        @DisplayName("Should handle null bio gracefully")
        void testSaveUserWithNullBio() {
            testUser.setBio(null);

            kafkaService.saveIntoUserDatabase(testUser);

            ArgumentCaptor<DataTransfer> dataTransferCaptor = ArgumentCaptor.forClass(DataTransfer.class);
            verify(kafkaTemplate).send(eq("SaveUserDatabase"), dataTransferCaptor.capture());

            DataTransfer sentData = dataTransferCaptor.getValue();
            assertThat(sentData.getMap()).containsEntry(KafkaDataTransferFields.BIO.toString(), null);
        }

        @Test
        @DisplayName("Should use correct topic name")
        void testCorrectTopicNameIsUsed() {
            kafkaService.saveIntoUserDatabase(testUser);

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(topicCaptor.capture(), ArgumentCaptor.forClass(DataTransfer.class).capture());

            assertThat(topicCaptor.getValue()).isEqualTo("SaveUserDatabase");
        }
    }
}
