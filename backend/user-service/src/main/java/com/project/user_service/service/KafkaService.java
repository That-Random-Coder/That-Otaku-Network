package com.project.user_service.service;

import com.project.user_service.domain.dto.kafka.DataTransfer;
import com.project.user_service.domain.dto.response.GroupResponseDto;
import com.project.user_service.domain.entity.groups.Group;
import com.project.user_service.domain.entity.users.Users;
import com.project.user_service.domain.enums.KafkaDataTransferFields;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class KafkaService {

    private KafkaTemplate<String, DataTransfer> kafkaTemplate;
    private static final String userTopic = "SaveUserDatabase";

    public void saveIntoUserDatabase(Users users) {

        Map<String, String> map = new HashMap<>();
        map.put(KafkaDataTransferFields.ID.toString(), users.getId().toString());
        map.put(KafkaDataTransferFields.USERNAME.toString(), users.getUsername());
        map.put(KafkaDataTransferFields.DISPLAY_NAME.toString(), users.getDisplayName());
        map.put(KafkaDataTransferFields.BIO.toString(), users.getBio());

        DataTransfer transfer = DataTransfer
                .builder()
                .type(KafkaDataTransferFields.USER_SAVE.toString())
                .map(map)
                .build();

        try {
            kafkaTemplate.send(userTopic, transfer);
            log.info("User is send with ID ; {} and Username : {}", users.getId().toString(), users.getUsername());
        } catch (Exception e) {
            log.error("User saved is Failed with ID : {} and Username : {}", users.getId().toString(),
                    users.getUsername());
            log.error(e.getMessage());
        }
    }

    public void updateIntoUserDatabase(UUID id, String displayName, String bio, String username) {

        Map<String, String> map = new HashMap<>();
        map.put(KafkaDataTransferFields.ID.toString(), id.toString());
        map.put(KafkaDataTransferFields.DISPLAY_NAME.toString(), displayName);
        map.put(KafkaDataTransferFields.BIO.toString(), bio);

        DataTransfer transfer = DataTransfer
                .builder()
                .type(KafkaDataTransferFields.USER_UPDATE.toString())
                .map(map)
                .build();

        try {
            kafkaTemplate.send(userTopic, transfer);
            log.info("User is send with ID ; {} and Username : {} for Update", id.toString(), username);
        } catch (Exception e) {
            log.error("User Failed to update with ID : {} and Username : {}", id.toString(), username);
            log.error(e.getMessage());
        }
    }

    public void saveIntoGroupDatabase(GroupResponseDto group) {

        Map<String, String> map = new HashMap<>();
        map.put(KafkaDataTransferFields.ID.toString(), group.getId().toString());
        map.put(KafkaDataTransferFields.USERNAME.toString(), group.getGroupName());
        map.put(KafkaDataTransferFields.BIO.toString(), group.getGroupBio());
        map.put(KafkaDataTransferFields.LEADER_USERNAME.toString(), group.getLeaderUsername());
        map.put(KafkaDataTransferFields.LEADER_DISPLAYNAME.toString(), group.getLeaderDisplayName());

        DataTransfer transfer = DataTransfer
                .builder()
                .type(KafkaDataTransferFields.GROUP_SAVE.toString())
                .map(map)
                .build();

        try {
            kafkaTemplate.send(userTopic, transfer);
            log.info("Group is send with ID ; {} and Username : {}", group.getId().toString(), group.getGroupName());
        } catch (Exception e) {
            log.error("Group saved is Failed with ID : {} and Username : {}", group.getId().toString(),
                    group.getGroupName());
            log.error(e.getMessage());
        }
    }

    public void updateIntoGroupDatabase(UUID id, String groupUsername, String bio, String leaderUsername,
            String leaderDisplayName) {

        Map<String, String> map = new HashMap<>();
        map.put(KafkaDataTransferFields.ID.toString(), id.toString());
        map.put(KafkaDataTransferFields.USERNAME.toString(), groupUsername);
        map.put(KafkaDataTransferFields.BIO.toString(), bio);
        map.put(KafkaDataTransferFields.LEADER_USERNAME.toString(), leaderUsername);
        map.put(KafkaDataTransferFields.LEADER_DISPLAYNAME.toString(), leaderDisplayName);

        DataTransfer transfer = DataTransfer
                .builder()
                .type(KafkaDataTransferFields.GROUP_UPDATE.toString())
                .map(map)
                .build();

        try {
            kafkaTemplate.send(userTopic, transfer);
            log.info("Group is send with ID ; {} for Update", id.toString());
        } catch (Exception e) {
            log.error("Group update Failed with ID : {}", id.toString());
            log.error(e.getMessage());
        }
    }

}
