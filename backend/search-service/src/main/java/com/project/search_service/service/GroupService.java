package com.project.search_service.service;

import com.project.search_service.domain.dto.SearchGroupDto;
import com.project.search_service.domain.dto.GroupSearchResponseDto;
import com.project.search_service.domain.entity.Group;
import com.project.search_service.repository.GroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private static final int PAGE_LENGTH = 12;

    public void createGroup(SearchGroupDto requestDto) {

        Group group = Group
                .builder()
                .id(requestDto.getId())
                .groupName(requestDto.getGroupUsername())
                .bio(requestDto.getBio())
                .leaderUsername(requestDto.getLeaderUsername())
                .leaderDisplayName(requestDto.getLeaderDisplayName())
                .enable(true)
                .build();

        log.info("Group with Id : {} and Name : {} is Created", requestDto.getId(), requestDto.getGroupUsername());
        groupRepository.save(group);
    }

    public void updateGroup(SearchGroupDto requestDto) {
        Group group = groupRepository.findById(requestDto.getId()).orElse(null);

        if (group == null) {
            log.info("Group : {} not found", requestDto.getId());
            return;
        }

        java.util.List<String> updated = new java.util.ArrayList<>();

        if (requestDto.getGroupUsername() != null) {
            group.setGroupName(requestDto.getGroupUsername());
            updated.add("Group Name");
        }

        if (requestDto.getBio() != null) {
            group.setBio(requestDto.getBio());
            updated.add("Bio");
        }

        if (requestDto.getLeaderUsername() != null) {
            group.setLeaderUsername(requestDto.getLeaderUsername());
            updated.add("Leader Username");
        }

        if (requestDto.getLeaderDisplayName() != null) {
            group.setLeaderDisplayName(requestDto.getLeaderDisplayName());
            updated.add("Leader Display Name");
        }

        groupRepository.save(group);
        log.info("Group : {} ({}) update : {}", group.getId(), group.getGroupName(), updated.toString());
    }

    public Page<GroupSearchResponseDto> searchGroup(String keyword, int page) {
        if (page < 0) {
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, PAGE_LENGTH);
        return groupRepository.searchByKeyword(keyword, pageable);
    }

}
