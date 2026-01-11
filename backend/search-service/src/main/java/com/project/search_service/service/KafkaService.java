package com.project.search_service.service;

import com.project.search_service.domain.dto.SearchGroupDto;
import com.project.search_service.domain.dto.SearchUserDto;
import com.project.search_service.domain.dto.kafka.DataTransfer;
import com.project.search_service.domain.enums.KafkaDataTransferFields;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class KafkaService {

    private final UserService userService;
    private final GroupService groupService;

    public void createUser(DataTransfer transfer) {
        UUID id = UUID.fromString(transfer.getMap().get(KafkaDataTransferFields.ID.toString()));
        SearchUserDto requestDto = SearchUserDto
                .builder()
                .id(id)
                .username(transfer.getMap().get(KafkaDataTransferFields.USERNAME.toString()))
                .displayName(transfer.getMap().get(KafkaDataTransferFields.DISPLAY_NAME.toString()))
                .bio(transfer.getMap().get(KafkaDataTransferFields.BIO.toString()))
                .build();

        userService.createUser(requestDto);

    }

    public void updateUser(DataTransfer transfer) {
        UUID id = UUID.fromString(transfer.getMap().get(KafkaDataTransferFields.ID.toString()));
        SearchUserDto requestDto = SearchUserDto
                .builder()
                .id(id)
                .username(transfer.getMap().get(KafkaDataTransferFields.USERNAME.toString()))
                .displayName(transfer.getMap().get(KafkaDataTransferFields.DISPLAY_NAME.toString()))
                .bio(transfer.getMap().get(KafkaDataTransferFields.BIO.toString()))
                .build();

        userService.updateUser(requestDto);
    }

    public void saveGroup(DataTransfer transfer) {
        SearchGroupDto requestDto = SearchGroupDto
                .builder()
                .id(transfer.getMap().get(KafkaDataTransferFields.ID.toString()))
                .groupUsername(transfer.getMap().get(KafkaDataTransferFields.USERNAME.toString()))
                .bio(transfer.getMap().get(KafkaDataTransferFields.BIO.toString()))
                .leaderUsername(transfer.getMap().get(KafkaDataTransferFields.LEADER_USERNAME.toString()))
                .leaderDisplayName(transfer.getMap().get(KafkaDataTransferFields.LEADER_DISPLAYNAME.toString()))
                .build();

        groupService.createGroup(requestDto);
    }

    public void updateGroup(DataTransfer transfer) {
        SearchGroupDto requestDto = SearchGroupDto
                .builder()
                .id(transfer.getMap().get(KafkaDataTransferFields.ID.toString()))
                .groupUsername(transfer.getMap().get(KafkaDataTransferFields.USERNAME.toString()))
                .bio(transfer.getMap().get(KafkaDataTransferFields.BIO.toString()))
                .leaderUsername(transfer.getMap().get(KafkaDataTransferFields.LEADER_USERNAME.toString()))
                .leaderDisplayName(transfer.getMap().get(KafkaDataTransferFields.LEADER_DISPLAYNAME.toString()))
                .build();

        groupService.updateGroup(requestDto);
    }
}
