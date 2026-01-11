package com.project.search_service.controller;

import com.project.search_service.domain.dto.UserSearchResponseDto;
import com.project.search_service.domain.dto.GroupSearchResponseDto;
import com.project.search_service.service.UserService;
import com.project.search_service.service.GroupService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@AllArgsConstructor
public class SearchController {

    private final UserService userService;
    private final GroupService groupService;

    @GetMapping("/user/get")
    private ResponseEntity<Page<UserSearchResponseDto>> searchUser(
            @RequestParam int page, @RequestParam String keyword) {
        Page<UserSearchResponseDto> responseDtos = userService.searchUser(keyword, page);

        return new ResponseEntity<>(responseDtos, HttpStatus.OK);
    }

    @GetMapping("/group/get")
    private ResponseEntity<Page<GroupSearchResponseDto>> searchGroup(
            @RequestParam int page, @RequestParam String keyword) {
        Page<GroupSearchResponseDto> responseDtos = groupService.searchGroup(keyword, page);
        return new ResponseEntity<>(responseDtos, HttpStatus.OK);
    }

}
